package com.github.bjoernpetersen.jmusicbot.playback;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;

/**
 * Abstract Playback implementation providing a lock and an associated "done" condition, as well as
 * an implementation for the {@link #waitForFinish()} method and a {@link #markDone()} method.
 */
public abstract class AbstractPlayback implements Playback {

  @Nonnull
  private final Lock lock;
  @Nonnull
  private final Condition done;

  protected AbstractPlayback() {
    this.lock = new ReentrantLock();
    this.done = lock.newCondition();
  }

  @Nonnull
  protected final Lock getLock() {
    return lock;
  }

  @Nonnull
  protected final Condition getDone() {
    return done;
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
      done.await();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Signals all threads waiting for the {@link #getDone()} condition.
   */
  protected final void markDone() {
    getLock().lock();
    try {
      getDone().signalAll();
    } finally {
      getLock().unlock();
    }
  }

  @Override
  public void close() throws Exception {
    markDone();
  }
}
