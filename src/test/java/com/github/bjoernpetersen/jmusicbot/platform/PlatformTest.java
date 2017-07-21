package com.github.bjoernpetersen.jmusicbot.platform;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

public class PlatformTest {

  @Test
  void nonNull() {
    assertNotNull(Platform.get());
  }

  @Test
  void initOnce() {
    Platform platform = Platform.get();
    Platform other = Platform.get();
    assertSame(platform, other);
  }

  @Test
  void lockingWorks() {
    final int threads = Math.min(4, Runtime.getRuntime().availableProcessors());
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    Lock lock = new ReentrantLock();
    CyclicBarrier start = new CyclicBarrier(threads);
    Queue<Platform> results = new ConcurrentLinkedQueue<>();
    for (int i = 0; i < threads; ++i) {
      int index = i;
      executor.submit(() -> {
        try {
          start.await();
        } catch (InterruptedException | BrokenBarrierException e) {
          fail(e);
        }
        results.add(Platform.get());
      });
    }

    executor.shutdown();
    try {
      assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
      fail(e);
    }

    Platform first = results.poll();
    for (Platform platform : results) {
      assertSame(first, platform);
    }
  }
}
