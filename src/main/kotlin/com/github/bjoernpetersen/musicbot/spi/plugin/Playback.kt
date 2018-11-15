package com.github.bjoernpetersen.musicbot.spi.plugin

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

typealias PlaybackStateListener = (PlaybackState) -> Unit

enum class PlaybackState {
    PLAY, PAUSE // TODO add broken?
}

interface PlaybackFactory : Plugin

/**
 * Playback for a single song. Playback should not start before [play] is called the first time.
 */
interface Playback : AutoCloseable {

    /**
     * Provides the playback with a PlaybackStateListener.
     * The listener can be used to tell the player about external pause/resume events.
     */
    fun setPlaybackStateListener(listener: PlaybackStateListener) = Unit

    /**
     * Resumes the playback. This might be called if the playback is already active.
     */
    fun play()

    /**
     * Pauses the playback. This might be called if the playback is already paused.
     */
    fun pause()

    @Throws(InterruptedException::class)
    fun waitForFinish()
}

/**
 * Abstract Playback implementation providing a [lock] and an associated [done] condition,
 * as well as an implementation for the [waitForFinish] method and a [markDone] method.
 *
 * @param lock A lock which will be used for critical code
 * @param done A condition which will come true when this Playback finishes
 */
abstract class AbstractPlayback private constructor(
    protected val lock: Lock,
    protected val done: Condition,
    private val _isDone: AtomicBoolean) : Playback {

    protected constructor() : this(ReentrantLock())
    private constructor(lock: Lock) : this(lock, lock.newCondition(), AtomicBoolean())

    protected var playbackListener: PlaybackStateListener? = null
        private set

    override fun setPlaybackStateListener(listener: PlaybackStateListener) {
        playbackListener = listener
    }

    protected fun isDone(): Boolean = _isDone.get()

    /**
     * Waits for the [done] condition.
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    @Throws(InterruptedException::class)
    override fun waitForFinish() {
        lock.withLock {
            while (!isDone()) {
                done.await()
            }
        }
    }

    /**
     * Signals all threads waiting for the [done] condition.
     */
    protected fun markDone() {
        lock.withLock {
            _isDone.set(true)
            done.signalAll()
        }
    }

    @Throws(Exception::class)
    override fun close() {
        markDone()
    }
}
