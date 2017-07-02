package com.github.bjoernpetersen.jmusicbot.playback;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Abstract Playback implementation providing a lock and an associated "done" condition, as well as
 * an implementation for the {@link #waitForFinish()} method and a {@link #markDone()} method.
 */
public abstract class AbstractPlayback implements Playback {

  @Nullable
  private PlaybackStateListener playbackStateListener;

  @Nonnull
  private final Lock lock;
  @Nonnull
  private final AtomicBoolean isDone;
  @Nonnull
  private final Condition done;

  protected AbstractPlayback() {
    this.lock = new ReentrantLock();
    this.isDone = new AtomicBoolean();
    this.done = lock.newCondition();
  }

  @Override
  public final void setPlaybackStateListener(
      @Nullable PlaybackStateListener playbackStateListener) {
    this.playbackStateListener = playbackStateListener;
  }

  @Nonnull
  protected final Optional<PlaybackStateListener> getPlaybackStateListener() {
    return Optional.ofNullable(playbackStateListener);
  }

  @Nonnull
  protected final Lock getLock() {
    return lock;
  }

  @Nonnull
  protected final Condition getDone() {
    return done;
  }

  protected final boolean isDone() {
    return isDone.get();
  }

  /**
   * Waits for the {@link #getDone()} condition.
   *
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  @Override
  public void waitForFinish() throws InterruptedException {
    Lock lock = getLock();
    Condition done = getDone();

    lock.lock();
    try {
      while (!isDone()) {
        done.await();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Signals all threads waiting for the {@link #getDone()} condition.
   */
  protected final void markDone() {
    Lock lock = getLock();
    Condition done = getDone();

    lock.lock();
    try {
      isDone.set(true);
      done.signalAll();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void close() throws Exception {
    markDone();
  }
}
