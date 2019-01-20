package net.bjoernpetersen.musicbot.internal.player

import com.google.common.util.concurrent.ThreadFactoryBuilder
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.Song
import net.bjoernpetersen.musicbot.api.player.DefaultSuggester
import net.bjoernpetersen.musicbot.api.player.ErrorState
import net.bjoernpetersen.musicbot.api.player.PauseState
import net.bjoernpetersen.musicbot.api.player.PlayState
import net.bjoernpetersen.musicbot.api.player.PlayerState
import net.bjoernpetersen.musicbot.api.player.QueueEntry
import net.bjoernpetersen.musicbot.api.player.SongEntry
import net.bjoernpetersen.musicbot.api.player.StopState
import net.bjoernpetersen.musicbot.api.player.SuggestedSongEntry
import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.spi.loader.SongLoader
import net.bjoernpetersen.musicbot.spi.player.Player
import net.bjoernpetersen.musicbot.spi.player.PlayerStateListener
import net.bjoernpetersen.musicbot.spi.player.QueueChangeListener
import net.bjoernpetersen.musicbot.spi.player.SongPlayedNotifier
import net.bjoernpetersen.musicbot.spi.player.SongQueue
import net.bjoernpetersen.musicbot.spi.plugin.BrokenSuggesterException
import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackState
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Named
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

internal class DefaultPlayer @Inject private constructor(
    private val queue: SongQueue,
    private val songLoader: SongLoader,
    private val pluginFinder: PluginFinder,
    private val songPlayedNotifier: SongPlayedNotifier,
    @Named("PluginClassLoader")
    private val classLoader: ClassLoader,
    defaultSuggester: DefaultSuggester) : Player {

    private val logger = KotlinLogging.logger {}
    private val autoPlayer: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("playerPool-%d")
            .build()
    )

    private val suggester: Suggester? = defaultSuggester.suggester
    private val stateLock: Lock = ReentrantLock()

    /**
     * The current state of this player. This might be play, pause, stop or error.
     */
    override var state: PlayerState = StopState
        private set(value) {
            field = value
            logger.debug { "Now playing ${value.entry}" }
            for (listener in stateListeners) {
                listener(value)
            }
        }
    private var playback: Playback = DummyPlayback

    private val stateListeners: MutableSet<PlayerStateListener> = HashSet()

    init {
        queue.addListener(object : QueueChangeListener {
            override fun onAdd(entry: QueueEntry) {
                startLoading(entry.song)
            }

            override fun onRemove(entry: QueueEntry) {}

            override fun onMove(entry: QueueEntry, fromIndex: Int, toIndex: Int) {}
        })

        if (suggester != null) {
            addListener { preloadSuggestion(suggester) }
        }
    }

    override fun start() {
        autoPlayer.submit { this.autoPlay() }
    }

    private fun Song.findProvider(): Provider? {
        val base = try {
            @Suppress("UNCHECKED_CAST")
            classLoader.loadClass(provider.id).kotlin as KClass<Provider>
        } catch (e: ClassNotFoundException) {
            logger.error(e) { "Could not find provider class for song" }
            return null
        } catch (e: ClassCastException) {
            logger.error(e) { "Could not cast ID base to KClass<Provider>" }
            return null
        }
        val provider: Provider? = pluginFinder[base]
        if (provider == null) {
            logger.error { "Could not find provider for class ${base.qualifiedName}" }
            return null
        }
        return provider
    }

    private fun startLoading(song: Song) {
        song.findProvider()?.let {
            songLoader.startLoading(it, song)
        }
    }

    private fun preloadSuggestion(suggester: Suggester) {
        if (queue.isEmpty) {
            val suggestions: List<Song>
            try {
                suggestions = suggester.getNextSuggestions(1)
            } catch (e: BrokenSuggesterException) {
                return
            }

            startLoading(suggestions[0])
        }
    }

    /**
     * Adds a [PlayerStateListener] which will be called everytime the [state] changes.
     */
    override fun addListener(listener: PlayerStateListener) {
        stateListeners.add(listener)
    }

    /**
     * Removes a [PlayerStateListener] previously registered with [addListener].
     */
    override fun removeListener(listener: PlayerStateListener) {
        stateListeners.remove(listener)
    }

    /**
     * Pauses the player.
     *
     * If the player is not currently playing anything, nothing will be done.
     *
     * This method blocks until the playback is paused.
     */
    @Throws(InterruptedException::class)
    override fun pause() {
        if (!stateLock.tryLock()) return
        try {
            logger.debug("Pausing...")
            val state = state
            if (state is PauseState) {
                logger.trace("Already paused.")
                return
            } else if (state !is PlayState) {
                logger.info { "Tried to pause player in state $state" }
                return
            }
            playback.pause()
            this.state = state.pause()
        } finally {
            stateLock.unlock()
        }
    }

    /**
     * Resumes the playback.
     *
     * If the player is not currently pausing, nothing will be done.
     *
     * This method blocks until the playback is resumed.
     */
    @Throws(InterruptedException::class)
    override fun play() {
        if (!stateLock.tryLock()) return
        try {
            logger.debug("Playing...")
            val state = state
            if (state is PlayState) {
                logger.trace("Already playing.")
                return
            } else if (state !is PauseState) {
                logger.info { "Tried to play in state $state" }
                return
            }
            playback.play()
            this.state = state.play()
        } finally {
            stateLock.unlock()
        }
    }

    /**
     * Plays the next song.
     *
     * This method will play the next song from the queue.
     * If the queue is empty, the next suggested song from the primary [suggester] will be used.
     * If there is no primary suggester, the player will transition into the [StopState].
     *
     * This method blocks until either a new song is playing or the StopState is reached.
     */
    @Throws(InterruptedException::class)
    override fun next() {
        val state = this.state
        stateLock.withLock {
            logger.debug("Next...")
            val newState = this.state
            if (state.entry !== newState.entry) {
                logger.debug("Skipping next call due to state change while waiting for lock.")
                return
            }

            try {
                playback.close()
            } catch (e: Exception) {
                logger.warn(e) { "Error closing playback" }
            }

            val next = queue.pop()
            if (next == null && suggester == null) {
                logger.info("Queue is empty. Stopping.")
                playback = DummyPlayback
                this.state = StopState
                return
            }

            val nextEntry: SongEntry = next ?: try {
                SuggestedSongEntry(suggester!!.suggestNext())
            } catch (e: BrokenSuggesterException) {
                logger.warn("Default suggester could not suggest anything. Stopping.")
                playback = DummyPlayback
                this.state = StopState
                return
            }

            val nextSong = nextEntry.song
            songPlayedNotifier.notifyPlayed(nextEntry)
            logger.debug("Next song is: $nextSong")
            try {
                playback = nextSong
                    .findProvider()
                    ?.getPlaybackSupplier(nextSong)
                    ?.supply(nextSong)
                    ?: throw IOException()
            } catch (e: IOException) {
                logger.warn(e) { "Error creating playback" }

                this.state = ErrorState
                playback = DummyPlayback
                return
            }

            playback.setPlaybackStateListener { this.onPlaybackFeedback(it) }

            this.state = PauseState(nextEntry)
            play()
        }
    }

    private fun onPlaybackFeedback(feedback: PlaybackState) {
        logger.trace { "Playback state update: $feedback" }
        stateLock.withLock {
            val state = state
            when (feedback) {
                PlaybackState.PLAY -> if (state is PauseState) {
                    logger.debug { "Changed to PLAY by Playback request" }
                    this.state = state.play()
                }
                PlaybackState.PAUSE -> if (state is PlayState) {
                    logger.debug { "Changed to PAUSE by Playback request" }
                    this.state = state.pause()
                }
                PlaybackState.BROKEN -> if (state !is ErrorState) {
                    logger.error { "Playback broke: ${playback::class.qualifiedName}" }
                    this.state = ErrorState
                    next()
                }
            }
        }
    }

    private fun autoPlay() {
        try {
            val state = this.state
            logger.debug("Waiting for song to finish")
            playback.waitForFinish()
            logger.trace("Waiting done")

            stateLock.withLock {
                // Prevent auto next calls if next was manually called
                if (this.state.entry !== state.entry) {
                    logger.debug("Skipping auto call to next()")
                } else {
                    logger.debug("Auto call to next()")
                    next()
                }
            }

            autoPlayer.submit { this.autoPlay() }
        } catch (e: InterruptedException) {
            logger.info("autoPlay interrupted", e)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        stateLock.withLock {
            try {
                autoPlayer.shutdownNow()
                playback.close()
                state = StopState
            } catch (e: Exception) {
                throw IOException(e)
            }
        }
    }
}

private object DummyPlayback : Playback {
    override fun play() {}

    override fun pause() {}

    @Throws(InterruptedException::class)
    override fun waitForFinish() {
        Thread.sleep(2000)
    }

    override fun close() {}
}
