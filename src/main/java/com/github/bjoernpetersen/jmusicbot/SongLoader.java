package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <p>Prepares songs to be played by a {@link PlaybackFactory}.</p>
 *
 * <p>Implementations are generally provided by a {@link Provider}. As an example, an implementation
 * could download a song to a MP3 file so a PlaybackFactory can subsequently play the file.</p>
 *
 * <p>It is possible that no loading is need or even possible before the song is actually played. In
 * this case, the {@link #DUMMY} implementation can be used.</p>
 */
public abstract class SongLoader {

  @Nonnull
  private static final Logger log = Logger.getLogger(SongLoader.class.getName());
  @Nullable
  private static ExecutorService service = null;

  /**
   * An implementation of SongLoader which does nothing.
   */
  @Nonnull
  public static final SongLoader DUMMY = new SongLoader() {
    @Override
    protected boolean loadImpl(@Nonnull Song song) {
      return true;
    }
  };

  @Nonnull
  private final Lock futureLock;
  @Nonnull
  private final Cache<Song, Future<Boolean>> futures;

  public SongLoader() {
    this.futureLock = new ReentrantLock();
    futures = CacheBuilder.newBuilder()
        .initialCapacity(8)
        .maximumSize(128)
        .weakKeys()
        .removalListener((RemovalListener<Song, Future<Boolean>>) removalNotification -> {
          Future<Boolean> future = removalNotification.getValue();
          if (!future.isDone()) {
            log.warning("Cancelling future because of cache removal");
            future.cancel(true);
          }
        })
        .build();
  }

  /**
   * <p>Whether the specified song has been successfully loaded.</p>
   *
   * <p>This method will block until the loading is done.</p>
   *
   * @param song a Song
   * @return whether loading was successful
   * @throws InterruptedException if the thread is interrupted while waiting for loading
   * @throws IllegalStateException if the song is not scheduled for loading
   */
  public final boolean hasLoaded(@Nonnull Song song) throws InterruptedException {
    Future<Boolean> future = futures.getIfPresent(song);
    if (future != null) {
      try {
        return !future.isCancelled() && future.get();
      } catch (ExecutionException e) {
        // ignore.
      }
    } else {
      throw new IllegalStateException("song is not loading");
    }

    return false;
  }

  /**
   * Asynchronously loads the specified song.
   *
   * @param song the song to load
   */
  public final void load(@Nonnull Song song) {
    if (futures.getIfPresent(song) == null) {
      futureLock.lock();
      try {
        if (futures.getIfPresent(song) == null) {
          log.finer("Enqueuing song load: " + song);
          futures.put(song, getService().submit(() -> this.loadImpl(song)));
        }
      } finally {
        futureLock.unlock();
      }
    }
  }

  /**
   * <p>Loads the song.</p>
   *
   * <p>This method might be called for several songs at the same time.</p>
   *
   * @param song a song to load
   * @return whether loading was successful
   */
  protected abstract boolean loadImpl(@Nonnull Song song);

  @Nonnull
  private static ExecutorService getService() {
    if (service == null) {
      service = Executors.newFixedThreadPool(2, new NamedThreadFactory("SongLoaderPool"));
    }
    return service;
  }

  static void reset() {
    getService().shutdownNow();
    service = null;
  }
}
