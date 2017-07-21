package com.github.bjoernpetersen.jmusicbot.playback;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AbstractPlaybackTest {

  private AbstractPlayback uut;

  @BeforeEach
  void init() {
    uut = new TestPlayback();
  }

  @Test
  void waitIndefinitely() {
    Assertions.assertThrows(AssertionError.class,
        () -> assertTimeoutPreemptively(ofMillis(500), () -> uut.waitForFinish())
    );
  }

  private static List<Callback<AbstractPlayback>> doneCalls() {
    return Arrays.asList(AbstractPlayback::markDone, AbstractPlayback::close);
  }

  @ParameterizedTest
  @MethodSource(names = "doneCalls")
  void waitFinishesOnDone(Callback<AbstractPlayback> doneCall) throws InterruptedException {
    Lock lock = new ReentrantLock();
    AtomicBoolean success = new AtomicBoolean(false);
    Condition called = lock.newCondition();
    Thread waiter = new Thread(() -> {
      try {
        uut.waitForFinish();
        success.set(true);
      } catch (InterruptedException e) {
        // ignore, leave success on false
      } finally {
        lock.lock();
        try {
          called.signalAll();
        } finally {
          lock.unlock();
        }
      }
    });
    waiter.start();

    lock.lock();
    try {
      assertTimeoutPreemptively(ofMillis(500), () -> doneCall.call(uut));
      assertTrue(called.await(500, TimeUnit.MILLISECONDS));
    } finally {
      lock.unlock();
      waiter.interrupt();
    }

    assertTrue(success.get());
  }

  @FunctionalInterface
  private interface Callback<T> {

    void call(T t) throws Exception;
  }


  private static class TestPlayback extends AbstractPlayback {

    @Override
    public void play() {
    }

    @Override
    public void pause() {
    }
  }
}
