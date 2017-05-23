package com.github.bjoernpetersen.jmusicbot;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class SongLoaderTest {

  private Song mockSong() {
    return mock(Song.class);
  }

  @Test
  void hasLoadedNotLoading() {
    SongLoader loader = new SongLoader() {
      @Override
      protected boolean loadImpl(@Nonnull Song song) {
        return true;
      }
    };

    Song song = mockSong();
    assertThrows(
        IllegalStateException.class,
        () -> assertTimeoutPreemptively(ofMillis(500), () -> loader.hasLoaded(song))
    );
  }

  @Test
  void hasLoadedSuccessful() throws InterruptedException {
    SongLoader loader = new SongLoader() {
      @Override
      protected boolean loadImpl(@Nonnull Song song) {
        return true;
      }
    };

    Song song = mockSong();
    loader.load(song);
    assertTrue(assertTimeoutPreemptively(ofMillis(500), () -> loader.hasLoaded(song)));
  }


  @Test
  void hasLoadedUnsuccessful() throws InterruptedException {
    SongLoader loader = new SongLoader() {
      @Override
      protected boolean loadImpl(@Nonnull Song song) {
        return false;
      }
    };

    Song song = mockSong();
    loader.load(song);
    assertFalse(assertTimeoutPreemptively(ofMillis(500), () -> loader.hasLoaded(song)));
  }

  @Test
  void loadTriggersLoader() throws InterruptedException {
    Lock lock = new ReentrantLock();
    Condition loaded = lock.newCondition();

    SongLoader loader = new SongLoader() {
      @Override
      protected boolean loadImpl(@Nonnull Song song) {
        lock.lock();
        try {
          loaded.signalAll();
        } finally {
          lock.unlock();
        }
        return true;
      }
    };

    Song song = mockSong();
    assertTimeoutPreemptively(ofMillis(500), () -> {
      lock.lock();
      try {
        loader.load(song);
        loaded.await();
        assertTrue(loader.hasLoaded(song));
      } finally {
        lock.unlock();
      }
    });

  }

  @AfterAll
  static void resetSongLoader() {
    SongLoader.reset();
  }
}
