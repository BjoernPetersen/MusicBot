package net.bjoernpetersen.musicbot.internal.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private sealed class PlayerMessage
private sealed class FeedbackPlayerMessage<T>(val response: CompletableDeferred<T>) :
    PlayerMessage()

private data class StateChange(
    val oldState: PlayerState,
    val changeState: suspend () -> Unit
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

/**
 * A player implementation that performs no synchronization at all, so it is not thread safe.
 *
 * This player should be used by a "synchronizing" player like [ActorPlayer], note that the actor
 * needs to be passed via [setActor] before using an instance of this class.
 */
private class SyncPlayer @Inject private constructor(
    private val queue: SongQueue,
    defaultSuggester: DefaultSuggester,
    private val resourceCache: ResourceCache,
    private val pluginLookup: PluginLookup,
    private val songPlayedNotifier: SongPlayedNotifier
) : Player {

    private val logger = KotlinLogging.logger {}

    private val stateListeners: MutableSet<PlayerStateListener> = HashSet()

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
    private lateinit var actor: SendChannel<PlayerMessage>

    private val suggester: Suggester? = defaultSuggester.suggester

    fun setActor(actor: SendChannel<PlayerMessage>) {
        this.actor = actor
    }

    private suspend fun applyFeedback(feedback: PlaybackState) {
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
                next()
            }
        }
    }

    suspend fun awaitCurrentPlayback() {
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
        val oldState = state
        when (oldState) {
            is PlayState -> logger.debug { "Already playing." }
            !is PauseState -> logger.info { "Tried to play in state $oldState" }
            else -> {
                playback.play()
                state = oldState.play()
            }
        }
    }

    override suspend fun pause() {
        logger.debug("Pausing...")
        val oldState = state
        when (oldState) {
            is PauseState -> logger.debug { "Already paused." }
            !is PlayState -> logger.info { "Tried to pause player in state $oldState" }
            else -> {
                playback.pause()
                state = oldState.pause()
            }
        }
    }

    override suspend fun next() {
        logger.debug("Next...")
        try {
            playback.close()
        } catch (e: Exception) {
            logger.warn(e) { "Error closing playback" }
        }

        val nextQueueEntry = queue.pop()
        if (nextQueueEntry == null && suggester == null) {
            if (state !is StopState) logger.info("Queue is empty. Stopping.")
            playback = DummyPlayback
            state = StopState
            return
        }

        val nextEntry: SongEntry = nextQueueEntry ?: try {
            SuggestedSongEntry(suggester!!.suggestNext())
        } catch (e: BrokenSuggesterException) {
            logger.warn("Default suggester could not suggest anything. Stopping.")
            playback = DummyPlayback
            state = StopState
            return
        }

        val nextSong = nextEntry.song
        songPlayedNotifier.notifyPlayed(nextEntry)
        logger.debug { "Next song is: $nextSong" }
        try {
            val resource = resourceCache.get(nextSong)
            val provider = pluginLookup
                .lookup(nextSong.provider)
                ?: throw Exception("No such provider: ${nextSong.provider}")
            playback = provider.supplyPlayback(nextSong, resource)
        } catch (e: Exception) {
            logger.warn(e) { "Error creating playback" }

            state = ErrorState
            playback = DummyPlayback
            return
        }

        playback.setPlaybackStateListener { feedback ->
            logger.trace { "Playback state update: $feedback" }
            try {
                actor.send(StateChange(state) { applyFeedback(feedback) })
            } catch (e: CancellationException) {
                logger.warn(e) { "Could not send playback feedback to actor" }
            }
        }
        state = PauseState(nextEntry)
        play()
    }

    override suspend fun close() {
        try {
            playback.close()
        } catch (e: Exception) {
            logger.error(e) { "Could not close playback" }
        }
        state = StopState
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
internal class ActorPlayer @Inject private constructor(
    private val syncPlayer: SyncPlayer,
    private val queue: SongQueue,
    defaultSuggester: DefaultSuggester,
    private val resourceCache: ResourceCache
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
            try {
                when (msg) {
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
                        } else msg.changeState()
                    }
                }
            } catch (e: Throwable) {
                if (msg is FeedbackPlayerMessage<*>) {
                    msg.response.completeExceptionally(e)
                }
                if (e is CancellationException) {
                    throw e
                }
                logger.error(e) { "Error in ActorPlayer actor loop" }
            }
        }
    }

    init {
        syncPlayer.setActor(actor)
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
            while (isActive) {
                val previousState = state
                logger.debug("Waiting for song to finish")
                syncPlayer.awaitCurrentPlayback()
                logger.trace("Waiting done")

                // Prevent auto next calls if next was manually called
                if (state !== previousState) {
                    logger.debug("Skipping auto call to next()")
                } else {
                    logger.debug("Auto call to next()")
                    next()
                }
            }
        }
    }

    override suspend fun close() {
        withContext(coroutineContext) {
            val result = CompletableDeferred<Unit>()
            actor.send(Stop(result))
            actor.close()
            result.await()
        }
        job.cancel()
    }
}
