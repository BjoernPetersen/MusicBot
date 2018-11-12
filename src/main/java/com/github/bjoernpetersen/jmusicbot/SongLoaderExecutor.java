package com.github.bjoernpetersen.jmusicbot;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SongLoaderExecutor {

  private static volatile SongLoaderExecutor instance;

  @Nonnull
  private final ExecutorService service;
  @Nonnull
  private final Logger logger = LoggerFactory.getLogger(SongLoaderExecutor.class);
  @Nonnull
  private final Lock futureLock;
  @Nonnull
  private final Cache<Song, Future<Boolean>> futures;

  private SongLoaderExecutor() {
    this.service = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("SongLoaderPool-%d")
        .build()
    );
    this.futureLock = new ReentrantLock();
    futures = CacheBuilder.newBuilder()
        .initialCapacity(8)
        .maximumSize(128)
        .weakKeys()
        .removalListener((RemovalListener<Song, Future<Boolean>>) removalNotification -> {
          Future<Boolean> future = removalNotification.getValue();
          if (!future.isDone()) {
            logger.warn("Cancelling future because of cache removal");
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
   * Asynchronously executed the SongLoader for the specified Song.
   *
   * @param song a Song
   */
  public final void execute(@Nonnull Song song) {
    if (futures.getIfPresent(song) == null) {
      futureLock.lock();
      try {
        if (futures.getIfPresent(song) == null) {
          logger.debug("Enqueuing song load: " + song);
          futures.put(song, service.submit(() -> song.getLoader().load(song)));
        }
      } finally {
        futureLock.unlock();
      }
    }
  }

  public void close() {
    service.shutdownNow();
    SongLoaderExecutor.instance = null;
  }

  static SongLoaderExecutor getInstance() {
    if (instance == null) {
      synchronized (SongLoaderExecutor.class) {
        if (instance == null) {
          instance = new SongLoaderExecutor();
        }
      }
    }
    return instance;
  }
}
