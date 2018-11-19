package com.github.bjoernpetersen.musicbot.internal.player

import com.github.bjoernpetersen.musicbot.api.Song
import com.github.bjoernpetersen.musicbot.api.player.ErrorState
import com.github.bjoernpetersen.musicbot.api.player.PauseState
import com.github.bjoernpetersen.musicbot.api.player.PlayState
import com.github.bjoernpetersen.musicbot.api.player.PlayerState
import com.github.bjoernpetersen.musicbot.api.player.QueueEntry
import com.github.bjoernpetersen.musicbot.api.player.SongEntry
import com.github.bjoernpetersen.musicbot.api.player.StopState
import com.github.bjoernpetersen.musicbot.api.player.SuggestedSongEntry
import com.github.bjoernpetersen.musicbot.spi.loader.SongLoader
import com.github.bjoernpetersen.musicbot.spi.player.Player
import com.github.bjoernpetersen.musicbot.spi.player.PlayerStateListener
import com.github.bjoernpetersen.musicbot.spi.player.QueueChangeListener
import com.github.bjoernpetersen.musicbot.spi.player.SongPlayedNotifier
import com.github.bjoernpetersen.musicbot.spi.player.SongQueue
import com.github.bjoernpetersen.musicbot.spi.plugin.BrokenSuggesterException
import com.github.bjoernpetersen.musicbot.spi.plugin.Playback
import com.github.bjoernpetersen.musicbot.spi.plugin.PlaybackState
import com.github.bjoernpetersen.musicbot.spi.plugin.Provider
import com.github.bjoernpetersen.musicbot.spi.plugin.Suggester
import com.github.bjoernpetersen.musicbot.spi.plugin.management.PluginFinder
import com.google.common.util.concurrent.ThreadFactoryBuilder
import mu.KotlinLogging
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

internal class DefaultPlayer @Inject constructor(
    private val queue: SongQueue,
    private val songLoader: SongLoader,
    private val pluginFinder: PluginFinder,
    private val songPlayedNotifier: SongPlayedNotifier,
    private val suggester: Suggester?) : Player {

    private val logger = KotlinLogging.logger {}
    private val autoPlayer: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("playerPool-%d")
            .build()
    )

    private val stateLock: Lock = ReentrantLock()
    /**
     * The current state of this player. This might be play, pause, stop or error.
     */
    override var state: PlayerState = StopState
        private set(value) {
            field = value
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
            Class.forName(provider.id).kotlin
        } catch (e: ClassNotFoundException) {
            logger.error(e) { "Could not find provider class for song" }
            return null
        }
        val provider: Provider? = pluginFinder[base as KClass<Provider>]
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
        stateLock.withLock {
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
        stateLock.withLock {
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
            if (isSignificantlyDifferent(state, newState)) {
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
        logger.debug { "Playback state update: $feedback" }
        stateLock.withLock {
            val state = state
            when (feedback) {
                PlaybackState.PLAY -> if (state is PauseState) {
                    this.state = state.play()
                }
                PlaybackState.PAUSE -> if (state is PlayState) {
                    this.state = state.pause()
                }
            }
        }
    }

    private fun isSignificantlyDifferent(state: PlayerState, other: PlayerState): Boolean {
        val stateSong = state.entry
        val otherSong = other.entry
        // TODO check for correctness
        return ((otherSong == null) != (stateSong == null)) || ((otherSong != null) && (stateSong != otherSong))
    }

    private fun autoPlay() {
        try {
            val state = this.state
            logger.debug("Waiting for song to finish")
            playback.waitForFinish()
            logger.trace("Waiting done")

            stateLock.withLock {
                // Prevent auto next calls if next was manually called
                if (isSignificantlyDifferent(this.state, state)) {
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
        try {
            autoPlayer.shutdownNow()
            playback.close()
        } catch (e: Exception) {
            throw IOException(e)
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
