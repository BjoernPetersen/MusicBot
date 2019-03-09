package net.bjoernpetersen.musicbot.internal.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.player.DefaultSuggester
import net.bjoernpetersen.musicbot.api.player.ErrorState
import net.bjoernpetersen.musicbot.api.player.PauseState
import net.bjoernpetersen.musicbot.api.player.PlayState
import net.bjoernpetersen.musicbot.api.player.PlayerState
import net.bjoernpetersen.musicbot.api.player.QueueEntry
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.player.SongEntry
import net.bjoernpetersen.musicbot.api.player.StopState
import net.bjoernpetersen.musicbot.api.player.SuggestedSongEntry
import net.bjoernpetersen.musicbot.spi.loader.ResourceCache
import net.bjoernpetersen.musicbot.spi.player.Player
import net.bjoernpetersen.musicbot.spi.player.PlayerStateListener
import net.bjoernpetersen.musicbot.spi.player.QueueChangeListener
import net.bjoernpetersen.musicbot.spi.player.SongPlayedNotifier
import net.bjoernpetersen.musicbot.spi.player.SongQueue
import net.bjoernpetersen.musicbot.spi.plugin.BrokenSuggesterException
import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackState
import net.bjoernpetersen.musicbot.spi.plugin.PluginLookup
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultPlayer @Inject private constructor(
    private val queue: SongQueue,
    private val resourceCache: ResourceCache,
    private val pluginLookup: PluginLookup,
    private val songPlayedNotifier: SongPlayedNotifier,
    defaultSuggester: DefaultSuggester
) : Player, CoroutineScope {

    private val logger = KotlinLogging.logger {}

    private lateinit var job: Job
    @Suppress("EXPERIMENTAL_API_USAGE")
    override val coroutineContext: CoroutineContext
        get() = newSingleThreadContext("Player") + job

    private val suggester: Suggester? = defaultSuggester.suggester

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
                launch { resourceCache.get(entry.song) }
            }

            override fun onRemove(entry: QueueEntry) {}

            override fun onMove(entry: QueueEntry, fromIndex: Int, toIndex: Int) {}
        })

        if (suggester != null) {
            addListener { preloadSuggestion(suggester) }
        }
    }

    override fun start() {
        job = Job()
        launch {
            autoPlay()
        }
    }

    private fun preloadSuggestion(suggester: Suggester) {
        launch {
            if (queue.isEmpty) {
                val suggestions: List<Song> = try {
                    suggester.getNextSuggestions(1)
                } catch (e: BrokenSuggesterException) {
                    return@launch
                }
                resourceCache.get(suggestions[0])
            }
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
    override suspend fun pause() {
        withContext(coroutineContext) {
            logger.debug("Pausing...")
            val oldState = state
            when (oldState) {
                is PauseState -> logger.trace("Already paused.")
                !is PlayState -> logger.info { "Tried to pause player in state $oldState" }
                else -> {
                    playback.pause()
                    state = oldState.pause()
                }
            }
        }
    }

    /**
     * Resumes the playback.
     *
     * If the player is not currently pausing, nothing will be done.
     *
     * This method blocks until the playback is resumed.
     */
    override suspend fun play() {
        withContext(coroutineContext) {
            logger.debug("Playing...")
            val oldState = state
            when (oldState) {
                is PlayState -> logger.trace("Already playing.")
                !is PauseState -> logger.info { "Tried to play in state $oldState" }
                else -> {
                    playback.play()
                    state = oldState.play()
                }
            }
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
    override suspend fun next() {
        val preState = this.state
        withContext(coroutineContext) {
            logger.debug("Next...")
            val newState = state
            if (preState.entry !== newState.entry) {
                logger.debug("Skipping next call due to state change while waiting for lock.")
                return@withContext
            }

            try {
                playback.close()
            } catch (e: Exception) {
                logger.warn(e) { "Error closing playback" }
            }

            val next = queue.pop()
            if (next == null && suggester == null) {
                if (state !is StopState) logger.info("Queue is empty. Stopping.")
                playback = DummyPlayback
                state = StopState
                return@withContext
            }

            val nextEntry: SongEntry = next ?: try {
                SuggestedSongEntry(suggester!!.suggestNext())
            } catch (e: BrokenSuggesterException) {
                logger.warn("Default suggester could not suggest anything. Stopping.")
                playback = DummyPlayback
                state = StopState
                return@withContext
            }

            val nextSong = nextEntry.song
            songPlayedNotifier.notifyPlayed(nextEntry)
            logger.debug("Next song is: $nextSong")
            try {
                val resource = try {
                    resourceCache.get(nextSong)
                } catch (e: Exception) {
                    throw IOException(e)
                }
                playback = pluginLookup
                    .lookup(nextSong.provider)
                    .supplyPlayback(nextSong, resource)
            } catch (e: IOException) {
                logger.warn(e) { "Error creating playback" }

                state = ErrorState
                playback = DummyPlayback
                return@withContext
            }

            playback.setPlaybackStateListener { onPlaybackFeedback(it) }

            state = PauseState(nextEntry)
            play()
        }
    }

    private suspend fun onPlaybackFeedback(feedback: PlaybackState) {
        logger.trace { "Playback state update: $feedback" }
        withContext(coroutineContext) {
            val oldState = state
            when (feedback) {
                PlaybackState.PLAY -> if (oldState is PauseState) {
                    logger.debug { "Changed to PLAY by Playback request" }
                    state = oldState.play()
                }
                PlaybackState.PAUSE -> if (oldState is PlayState) {
                    logger.debug { "Changed to PAUSE by Playback request" }
                    state = oldState.pause()
                }
                PlaybackState.BROKEN -> if (oldState !is ErrorState) {
                    logger.error { "Playback broke: ${playback::class.qualifiedName}" }
                    state = ErrorState
                    next()
                }
            }
        }
    }

    private suspend fun autoPlay() {
        while (isActive) {
            val state = this.state
            logger.debug("Waiting for song to finish")
            playback.waitForFinish()
            logger.trace("Waiting done")

            // Prevent auto next calls if next was manually called
            if (this.state.entry !== state.entry) {
                logger.debug("Skipping auto call to next()")
            } else {
                logger.debug("Auto call to next()")
                next()
            }
        }
    }

    override suspend fun close() {
        withContext(coroutineContext) {
            try {
                playback.close()
            } catch (e: Exception) {
                logger.error(e) { "Could not close playback" }
            }
            state = StopState
            job.cancelAndJoin()
        }
    }
}

private object DummyPlayback : Playback {
    override suspend fun play() {}

    override suspend fun pause() {}

    override suspend fun waitForFinish() {
        delay(1000)
    }

    override suspend fun close() {}
}
