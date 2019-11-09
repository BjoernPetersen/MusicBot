package net.bjoernpetersen.musicbot.internal.player

import java.time.Duration
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.player.DefaultSuggester
import net.bjoernpetersen.musicbot.api.player.ErrorState
import net.bjoernpetersen.musicbot.api.player.PauseState
import net.bjoernpetersen.musicbot.api.player.PlayState
import net.bjoernpetersen.musicbot.api.player.PlayerState
import net.bjoernpetersen.musicbot.api.player.ProgressTracker
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
import net.bjoernpetersen.musicbot.spi.plugin.AbstractPlayback
import net.bjoernpetersen.musicbot.spi.plugin.BrokenSuggesterException
import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFeedbackChannel
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackState
import net.bjoernpetersen.musicbot.spi.plugin.PluginLookup
import net.bjoernpetersen.musicbot.spi.plugin.Suggester

private sealed class PlayerMessage
private sealed class FeedbackPlayerMessage<T>(val response: CompletableDeferred<T>) :
    PlayerMessage()

private class Start(response: CompletableDeferred<Unit>) : FeedbackPlayerMessage<Unit>(response)

private data class StateChange(
    val oldState: PlayerState,
    val feedback: PlaybackState
) : PlayerMessage()

private data class AddListener(val listener: PlayerStateListener) : PlayerMessage()
private data class RemoveListener(val listener: PlayerStateListener) : PlayerMessage()
private class Play(response: CompletableDeferred<Unit>) : FeedbackPlayerMessage<Unit>(response)
private class Pause(response: CompletableDeferred<Unit>) : FeedbackPlayerMessage<Unit>(response)
private class Stop(response: CompletableDeferred<Unit>) : FeedbackPlayerMessage<Unit>(response)
private class Next(
    val oldState: PlayerState,
    response: CompletableDeferred<Unit>
) : FeedbackPlayerMessage<Unit>(response)

private class Await(response: CompletableDeferred<Unit>) : FeedbackPlayerMessage<Unit>(response)

/**
 * A playback implementation that doesn't actually do anything. The only way it ever ends is if
 * [close] is called.
 */
private class CompletablePlayback : AbstractPlayback() {

    override suspend fun play() = Unit
    override suspend fun pause() = Unit
}

/**
 * A player implementation that performs no synchronization at all, so it is not thread safe.
 *
 * This player should be used by a "synchronizing" player like [ActorPlayer].
 */
private class SyncPlayer @Inject private constructor(
    private val queue: SongQueue,
    defaultSuggester: DefaultSuggester,
    private val resourceCache: ResourceCache,
    private val pluginLookup: PluginLookup,
    private val songPlayedNotifier: SongPlayedNotifier,
    private val playbackFeedbackChannel: PlaybackFeedbackChannel
) : Player {

    private val logger = KotlinLogging.logger {}

    private val stateListeners: MutableSet<PlayerStateListener> = HashSet()

    /**
     * The current state of this player. This might be play, pause, stop or error.
     */
    override var state: PlayerState = StopState
        private set(value) {
            val old = field
            field = value
            logger.debug { "Now playing ${value.entry}" }
            for (listener in stateListeners) {
                listener(old, value)
            }
        }
    private var playback: Playback = CompletablePlayback()

    private val suggester: Suggester? = defaultSuggester.suggester

    suspend fun applyStateFeedback(feedback: PlaybackState) {
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
            PlaybackState.BROKEN -> if (state !is ErrorState) {
                logger.error { "Playback broke: ${playback::class.qualifiedName}" }
                state = ErrorState
            }
        }
    }

    suspend fun awaitCurrentPlayback() {
        logger.debug { "Awaiting playback $playback" }
        playback.waitForFinish()
    }

    override fun start() {
        // This player isn't active in any way.
    }

    override fun addListener(listener: PlayerStateListener) {
        stateListeners.add(listener)
    }

    override fun removeListener(listener: PlayerStateListener) {
        stateListeners.remove(listener)
    }

    override suspend fun play() {
        logger.debug("Playing...")
        when (val oldState = state) {
            is PlayState -> logger.debug { "Already playing." }
            !is PauseState -> {
                logger.debug { "Calling next because of play call in state ${oldState.name}" }
                next()
            }
            else -> {
                playback.play()
                state = oldState.play()
            }
        }
    }

    override suspend fun pause() {
        logger.debug("Pausing...")
        when (val oldState = state) {
            is PauseState -> logger.debug { "Already paused." }
            !is PlayState -> logger.info { "Tried to pause player in state $oldState" }
            else -> {
                playback.pause()
                state = oldState.pause()
            }
        }
    }

    @Suppress("ReturnCount")
    override suspend fun next() {
        logger.debug("Next...")

        @Suppress("TooGenericExceptionCaught")
        try {
            logger.debug { "Closing playback $playback" }
            playback.close()
        } catch (e: Exception) {
            logger.warn(e) { "Error closing playback" }
        }

        val nextQueueEntry = queue.pop()
        if (nextQueueEntry == null && suggester == null) {
            if (state !is StopState) logger.info("Queue is empty. Stopping.")
            playback = CompletablePlayback()
            state = StopState
            return
        }

        val nextEntry: SongEntry = nextQueueEntry ?: try {
            SuggestedSongEntry(suggester!!.suggestNext())
        } catch (e: BrokenSuggesterException) {
            logger.warn("Default suggester could not suggest anything. Stopping.")
            playback = CompletablePlayback()
            state = ErrorState
            return
        }

        val nextSong = nextEntry.song
        songPlayedNotifier.notifyPlayed(nextEntry)
        logger.debug { "Next song is: $nextSong" }
        @Suppress("TooGenericExceptionCaught")
        try {
            val resource = resourceCache.get(nextSong)
            val provider = pluginLookup
                .lookup(nextSong.provider)
                ?: throw IllegalArgumentException("No such provider: ${nextSong.provider}")
            playback = provider.supplyPlayback(nextSong, resource)
        } catch (e: Throwable) {
            logger.warn(e) { "Error creating playback" }

            playback = CompletablePlayback()
            state = ErrorState
            return
        }

        playback.setFeedbackChannel(playbackFeedbackChannel)
        state = PauseState(nextEntry)
        play()
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun close() {
        try {
            playback.close()
        } catch (e: Exception) {
            logger.error(e) { "Could not close playback" }
        }
        playback = CompletablePlayback()
        state = ErrorState
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
internal class ActorPlayer @Inject private constructor(
    private val syncPlayer: SyncPlayer,
    private val queue: SongQueue,
    defaultSuggester: DefaultSuggester,
    private val resourceCache: ResourceCache,
    feedbackChannel: ActorPlaybackFeedbackChannel,
    progressTracker: ProgressTracker
) : Player, CoroutineScope {

    private val logger = KotlinLogging.logger {}

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val suggester: Suggester? = defaultSuggester.suggester

    override val state: PlayerState
        get() = syncPlayer.state

    private val actor = actor<PlayerMessage> {
        for (msg in channel) {
            @Suppress("TooGenericExceptionCaught")
            try {
                when (msg) {
                    is Start -> {
                        syncPlayer.play()
                        msg.response.complete(Unit)
                    }
                    is AddListener -> syncPlayer.addListener(msg.listener)
                    is RemoveListener -> syncPlayer.removeListener(msg.listener)
                    is Play -> {
                        syncPlayer.play()
                        msg.response.complete(Unit)
                    }
                    is Pause -> {
                        syncPlayer.pause()
                        msg.response.complete(Unit)
                    }
                    is Next -> {
                        if (syncPlayer.state !== msg.oldState) {
                            logger.debug { "Skipping next call due to state change" }
                        } else {
                            syncPlayer.next()
                        }
                        msg.response.complete(Unit)
                    }
                    is Stop -> {
                        syncPlayer.close()
                        msg.response.complete(Unit)
                    }
                    is StateChange -> {
                        if (syncPlayer.state !== msg.oldState) {
                            logger.debug { "Ignoring playback state due to state change" }
                        } else {
                            syncPlayer.applyStateFeedback(msg.feedback)
                        }
                    }
                    is Await -> {
                        launch {
                            syncPlayer.awaitCurrentPlayback()
                            msg.response.complete(Unit)
                        }
                    }
                }
            } catch (e: Throwable) {
                if (msg is FeedbackPlayerMessage<*>) {
                    msg.response.completeExceptionally(e)
                }
                // TODO remove
                logger.warn(e) { "May be cancellation in ActorPlayer actor loop" }
                if (e is CancellationException) {
                    throw e
                }
                logger.error(e) { "Error in ActorPlayer actor loop" }
            }
        }
    }

    init {
        feedbackChannel.onStateChange = { feedback ->
            logger.trace { "Playback state update: $feedback" }
            try {
                launch { actor.send(StateChange(state, feedback)) }
            } catch (e: CancellationException) {
                logger.warn(e) { "Could not send playback feedback to actor" }
            }
        }

        addListener { old, new ->
            launch {
                if (!old.hasSong() && new.hasSong()) {
                    progressTracker.startSong()
                }

                if (new.hasSong() && new.entry?.song != old.entry?.song) {
                    progressTracker.reset()
                    progressTracker.startSong()
                }

                when (new) {
                    is PauseState -> progressTracker.startPause()
                    is PlayState -> progressTracker.stopPause()
                    ErrorState, StopState -> progressTracker.reset()
                }
            }
        }

        queue.addListener(object : QueueChangeListener {
            override fun onAdd(entry: QueueEntry) {
                launch { resourceCache.get(entry.song) }
                if (state is StopState) launch { next() }
            }

            override fun onRemove(entry: QueueEntry) = Unit

            override fun onMove(entry: QueueEntry, fromIndex: Int, toIndex: Int) = Unit
        })

        if (suggester != null) {
            addListener { _, _ -> preloadSuggestion(suggester) }
        }
    }

    override fun start() {
        launch {
            val response = CompletableDeferred<Unit>()
            actor.send(Start(response))
            response.await()
            autoPlay()
        }
    }

    private fun preloadSuggestion(suggester: Suggester) {
        launch {
            if (queue.isEmpty) {
                val suggestions: List<Song> = try {
                    suggester.getNextSuggestions(1)
                } catch (e: BrokenSuggesterException) {
                    logger.warn(e) { "Default suggester could not suggest anything to preload" }
                    return@launch
                }
                resourceCache.get(suggestions[0])
            }
        }
    }

    override fun addListener(listener: PlayerStateListener) {
        launch {
            actor.send(AddListener(listener))
        }
    }

    override fun removeListener(listener: PlayerStateListener) {
        launch {
            actor.send(RemoveListener(listener))
        }
    }

    override suspend fun play() {
        withContext(coroutineContext) {
            val result = CompletableDeferred<Unit>()
            actor.send(Play(result))
            result.await()
        }
    }

    override suspend fun pause() {
        withContext(coroutineContext) {
            val result = CompletableDeferred<Unit>()
            actor.send(Pause(result))
            result.await()
        }
    }

    override suspend fun next() {
        val oldState = state
        withContext(coroutineContext) {
            val result = CompletableDeferred<Unit>()
            actor.send(Next(oldState, result))
            result.await()
        }
    }

    private suspend fun autoPlay() {
        withContext(coroutineContext) {
            while (!actor.isClosedForSend) {
                val previousState = state
                logger.debug("Waiting for song to finish")
                val await = CompletableDeferred<Unit>()
                actor.send(Await(await))
                await.await()
                logger.trace("Waiting done")

                if (actor.isClosedForSend) continue

                // Prevent auto next calls if next was manually called or an error occurred
                when {
                    state !== previousState ->
                        logger.debug("Skipping auto call to next() due to state change")
                    state is ErrorState ->
                        logger.debug("Skipping auto call to next() because player is in $state")
                    else -> {
                        logger.debug("Auto call to next()")
                        next()
                    }
                }
            }
        }
    }

    override suspend fun close() {
        withContext(coroutineContext) {
            job.complete()
            val result = CompletableDeferred<Unit>()
            actor.send(Stop(result))
            actor.close()
            result.await()
        }
        job.cancel()
    }
}

internal class ActorPlaybackFeedbackChannel @Inject private constructor(
    private val progressTracker: ProgressTracker
) : PlaybackFeedbackChannel {

    var onStateChange: ((PlaybackState) -> Unit)? = null
        set(value) {
            if (field != null) throw IllegalStateException("Already initialized")
            else field = value
        }

    override fun updateState(state: PlaybackState) {
        onStateChange?.let { it(state) }
    }

    override fun updateProgress(progress: Duration) {
        runBlocking {
            progressTracker.updateProgress(progress)
        }
    }
}
