package com.github.bjoernpetersen.jmusicbot.playback

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Abstract Playback implementation providing a [lock] and an associated [done] condition, as well as
 * an implementation for the [waitForFinish] method and a [markDone] method.
 */
abstract class AbstractPlayback private constructor(
    /**
     * A lock which will be used for critical code.
     */
    protected val lock: Lock,
    /**
     * A condition which will come true when this Playback finishes.
     */
    protected val done: Condition,
    private val _isDone: AtomicBoolean) : Playback {

  protected constructor() : this(ReentrantLock())
  private constructor(lock: Lock) : this(lock, lock.newCondition(), AtomicBoolean())

  private var playbackListener: PlaybackStateListener? = null

  override fun setPlaybackStateListener(listener: PlaybackStateListener) {
    playbackListener = listener
  }

  protected fun getPlaybackStateListener(): Optional<PlaybackStateListener> =
      Optional.ofNullable(playbackListener)

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
