package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.Loggable;
import com.github.bjoernpetersen.jmusicbot.Song;
import com.github.bjoernpetersen.jmusicbot.playback.PlayerState.State;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Player implements Loggable, Closeable {

  @Nonnull
  private final Logger logger;

  @Nonnull
  private final ExecutorService executorService;

  @Nonnull
  private final Consumer<SongEntry> songPlayedNotifier;

  @Nonnull
  private final Queue queue;
  @Nullable
  private final Suggester suggester;

  @Nonnull
  private final Lock stateLock;
  @Nonnull
  private PlayerState state;
  @Nonnull
  private Playback playback;

  @Nonnull
  private final Set<PlayerStateListener> stateListeners;

  public Player(@Nonnull Consumer<SongEntry> songPlayedNotifier, @Nullable Suggester suggester) {
    this.logger = createLogger();

    this.executorService = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("playerPool-%d")
            .build()
    );

    this.songPlayedNotifier = songPlayedNotifier;

    this.queue = new Queue();
    this.suggester = suggester;

    this.stateLock = new ReentrantLock();
    this.state = PlayerState.stop();
    this.playback = DummyPlayback.INSTANCE;

    this.stateListeners = new HashSet<>();

    executorService.submit(this::autoPlay);
  }

  @Override
  @Nonnull
  public Logger getLogger() {
    return logger;
  }

  public void addListener(@Nullable PlayerStateListener listener) {
    if (listener != null) {
      stateListeners.add(listener);
    }
  }

  public void removeListener(@Nullable PlayerStateListener listener) {
    if (listener != null) {
      stateListeners.remove(listener);
    }
  }

  @Nonnull
  public Queue getQueue() {
    return queue;
  }

  @Nonnull
  public PlayerState getState() {
    return state;
  }

  private void setState(PlayerState state) {
    this.state = state;
    for (PlayerStateListener listener : stateListeners) {
      listener.onChanged(state);
    }
  }

  public void pause() {
    Lock stateLock = this.stateLock;
    stateLock.lock();
    try {
      logFinest("Pausing...");
      PlayerState state = getState();
      if (state.getState() == State.PAUSE) {
        logFinest("Already paused.");
        return;
      } else if (state.getState() != State.PLAY) {
        logFiner("Tried to pause player in state %s", state.getState());
        return;
      }
      playback.pause();
      setState(PlayerState.pause(state.getEntry().orElseThrow(IllegalStateException::new)));
    } finally {
      stateLock.unlock();
    }
  }

  public void play() {
    Lock stateLock = this.stateLock;
    stateLock.lock();
    try {
      logFinest("Playing...");
      PlayerState state = getState();
      if (state.getState() == State.PLAY) {
        logFinest("Already playing.");
        return;
      } else if (state.getState() != State.PAUSE) {
        logFiner("Tried to play in state %s", state.getState());
        return;
      }
      playback.play();
      setState(PlayerState.play(state.getEntry().orElseThrow(IllegalStateException::new)));
    } finally {
      stateLock.unlock();
    }
  }

  public void next() throws InterruptedException {
    PlayerState state = getState();
    Lock stateLock = this.stateLock;
    stateLock.lock();
    try {
      logFiner("Next...");
      PlayerState newState = getState();
      if (isSignificantlyDifferent(state, newState)) {
        logFinest("Skipping next call due to state change while waiting for lock.");
        return;
      }

      try {
        playback.close();
      } catch (Exception e) {
        logWarning(e, "Error closing playback");
      }

      Optional<QueueEntry> nextOptional = queue.pop();
      if (!nextOptional.isPresent() && suggester == null) {
        logFinest("Queue is empty. Stopping.");
        playback = DummyPlayback.INSTANCE;
        setState(PlayerState.stop());
        return;
      }

      SongEntry nextEntry;
      if (nextOptional.isPresent()) {
        nextEntry = nextOptional.get();
      } else {
        nextEntry = new SuggestedSongEntry(suggester.suggestNext());
      }

      Song nextSong = nextEntry.getSong();
      songPlayedNotifier.accept(nextEntry);
      logFiner("Next song is: " + nextSong);
      try {
        playback = nextSong.getPlayback();
      } catch (IOException e) {
        logWarning(e, "Error creating playback");

        setState(PlayerState.error());
        playback = DummyPlayback.INSTANCE;
        return;
      }

      playback.setPlaybackStateListener(playbackState -> {
        logFinest("Playback state update: %s", playbackState);
        switch (playbackState) {
          case PLAY:
            play();
            break;
          case PAUSE:
            pause();
            break;
          default:
            throw new IllegalArgumentException("Unknown PlaybackState: " + playbackState);
        }
      });

      setState(PlayerState.pause(nextEntry));
      play();
    } finally {
      stateLock.unlock();
    }
  }

  private boolean isSignificantlyDifferent(@Nonnull PlayerState state, @Nonnull PlayerState other) {
    Optional<SongEntry> stateSong = state.getEntry();
    Optional<SongEntry> otherSong = other.getEntry();
    // TODO check for correctness
    return (otherSong.isPresent() != stateSong.isPresent())
        || otherSong.isPresent() && !state.getEntry().equals(other.getEntry());
  }

  private void autoPlay() {
    try {
      PlayerState state = getState();
      logFinest("Waiting for song to finish");
      playback.waitForFinish();
      logFinest("Waiting done");

      Lock stateLock = this.stateLock;
      stateLock.lock();
      try {
        // Prevent auto next calls if next was manually called
        if (isSignificantlyDifferent(getState(), state)) {
          logFinest("Skipping auto call to next()");
        } else {
          logFinest("Auto call to next()");
          next();
        }
      } finally {
        stateLock.unlock();
      }

      executorService.submit(this::autoPlay);
    } catch (InterruptedException e) {
      logFine("autoPlay interrupted", e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      executorService.shutdownNow();
      playback.close();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }
}
