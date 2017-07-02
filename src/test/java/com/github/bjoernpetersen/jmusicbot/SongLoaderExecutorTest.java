package com.github.bjoernpetersen.jmusicbot;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SongLoaderExecutorTest {

  private SongLoaderExecutor executor;

  private Song mockSong(SongLoader loader) {
    Song song = mock(Song.class);
    when(song.getLoader()).thenReturn(loader);
    return song;
  }

  @BeforeEach
  void getInstance() {
    executor = SongLoaderExecutor.getInstance();
  }

  @AfterEach
  void resetSongLoader() {
    executor.close();
    executor = null;
  }

  @Test
  void hasLoadedNotLoading() {
    Song song = mockSong(s -> true);
    assertThrows(
        IllegalStateException.class,
        () -> assertTimeoutPreemptively(ofMillis(500), () -> executor.hasLoaded(song))
    );
  }

  @Test
  void hasLoadedSuccessful() throws InterruptedException {
    Song song = mockSong(s -> true);
    executor.execute(song);
    assertTrue(assertTimeoutPreemptively(ofMillis(500), () -> executor.hasLoaded(song)));
  }


  @Test
  void hasLoadedUnsuccessful() throws InterruptedException {
    Song song = mockSong(s -> false);
    executor.execute(song);
    assertFalse(assertTimeoutPreemptively(ofMillis(500), () -> executor.hasLoaded(song)));
  }

  @Test
  void loadTriggersLoader() throws InterruptedException {
    Lock lock = new ReentrantLock();
    Condition loaded = lock.newCondition();

    SongLoader loader = song -> {
      lock.lock();
      try {
        loaded.signalAll();
      } finally {
        lock.unlock();
      }
      return true;
    };

    Song song = mockSong(loader);

    assertTimeoutPreemptively(ofMillis(500), () -> {
      lock.lock();
      try {
        executor.execute(song);
        loaded.await();
        assertTrue(executor.hasLoaded(song));
      } finally {
        lock.unlock();
      }
    });

  }
}
