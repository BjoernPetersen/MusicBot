package net.bjoernpetersen.musicbot.spi.plugin

import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

/**
 * A feedback channel back to the player to send signals about playback state changes
 * outside of the bot's control.
 *
 * All feedback options are entirely optional, i.e. [Playback] implementations are not required to
 * provide any feedback.
 */
interface PlaybackFeedbackChannel {

    /**
     * Notify the listener of a playback [state] change.
     *
     * For example, if the user paused the official Spotify client directly, the Spotify Playback may
     * detect this and signal the new [PlaybackState.PAUSE] state.
     *
     * This method may be called with the current state even if it is unchanged.
     * The listener won't react if the state hasn't actually changed.
     *
     * This method does **not** have to be called when one of the [Playback.play], [Playback.pause]
     * or [Playback.close] methods were called.
     *
     * ### Note
     * If the playback has finished, don't call this method, but rather release all waiting threads
     * from the [Playback.waitForFinish] method instead.
     */
    fun updateState(state: PlaybackState)

    /**
     * Update the current playback progress.
     */
    fun updateProgress(progress: Duration)
}

/**
 * A kind of Playback state.
 */
enum class PlaybackState {

    /**
     * The Playback is playing.
     */
    PLAY,
    /**
     * The playback is paused, but not stopped, finished, or broken.
     */
    PAUSE,
    /**
     * The playback is broken and won't be able to continue playing.
     *
     * The player will react to this by closing the playback and moving on to the next song.
     */
    BROKEN
}

/**
 * A plugin that provides playback objects for some media/input type.
 *
 * Note that this interface is relatively worthless by itself, because it doesn't have
 * a `createPlayback` method. The reason for that is, that the signature of such a method is highly
 * dependent on the capabilities of the media/input format a PlaybackFactory can handle.
 *
 * Have a look at the `predefined` subpackage for some subtypes that are sensible to implement or
 * depend on.
 */
interface PlaybackFactory : Plugin

/**
 * Playback for a single song. Playback should not start before [play] is called the first time.
 */
interface Playback {

    /**
     * Provides the playback with a feedback channel.
     * Using the channel is entirely optional.
     */
    fun setFeedbackChannel(channel: PlaybackFeedbackChannel) = Unit

    /**
     * Resumes the playback. This might be called if the playback is already playing.
     *
     * This is also called to initially start playing.
     */
    suspend fun play()

    /**
     * Pauses the playback. This might be called if the playback is already paused.
     */
    suspend fun pause()

    /**
     * Suspends the calling function until the playback has finished playing or is closed.
     */
    suspend fun waitForFinish()

    /**
     * Closes the playback, releases all resources held by it and triggers a release of all waiting
     * coroutines which called [waitForFinish].
     */
    @Throws(Exception::class)
    suspend fun close()
}

/**
 * Abstract Playback implementation providing a [done] condition,
 * as well as an implementation for the [waitForFinish] method and a [markDone] method.
 */
abstract class AbstractPlayback protected constructor() : Playback, CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val done = CompletableDeferred<Unit>()

    protected lateinit var feedbackChannel: PlaybackFeedbackChannel
        private set

    override fun setFeedbackChannel(channel: PlaybackFeedbackChannel) {
        feedbackChannel = channel
    }

    protected fun isDone(): Boolean = done.isCompleted

    /**
     * Waits for the [done] condition.
     */
    override suspend fun waitForFinish() {
        done.await()
    }

    /**
     * Signals all threads waiting for the [done] condition.
     */
    protected fun markDone() {
        done.complete(Unit)
    }

    @Throws(Exception::class)
    override suspend fun close() {
        withContext(coroutineContext) {
            markDone()
        }
        job.cancel()
    }
}
