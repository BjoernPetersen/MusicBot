package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.NamedThreadFactory;
import com.github.bjoernpetersen.jmusicbot.Song;
import com.github.bjoernpetersen.jmusicbot.playback.PlayerState.State;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class Player implements Closeable {

  private static final Logger log = Logger.getLogger(Player.class.getName());

  @Nonnull
  private final ExecutorService executorService;

  @Nonnull
  private final Consumer<Song> songPlayedNotifier;

  @Nonnull
  private final Queue queue;
  @Nonnull
  private final Suggester suggester;

  @Nonnull
  private final Lock stateLock;
  @Nonnull
  private PlayerState state;
  @Nonnull
  private Playback playback;

  @Nonnull
  private final Set<PlayerStateListener> stateListeners;

  public Player(Consumer<Song> songPlayedNotifier, Suggester suggester) {
    this.executorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("playerPool"));

    this.songPlayedNotifier = songPlayedNotifier;

    this.queue = new Queue();
    this.suggester = Objects.requireNonNull(suggester);

    this.stateLock = new ReentrantLock();
    this.state = PlayerState.stop();
    this.playback = DummyPlayback.getInstance();

    this.stateListeners = new HashSet<>();

    executorService.submit(this::autoPlay);
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
    for (PlayerStateListener listener : stateListeners) {
      listener.onChange(this.state, state);
    }
    this.state = state;
    for (PlayerStateListener listener : stateListeners) {
      listener.onChanged(state);
    }
  }

  public void pause() {
    Lock stateLock = this.stateLock;
    stateLock.lock();
    try {
      log.info("Pausing...");
      PlayerState state = getState();
      if (state.getState() == State.PAUSE) {
        log.finer("Already paused.");
        return;
      } else if (state.getState() != State.PLAY) {
        log.finer(String.format("Tried to pause player in state %s", state.getState()));
        return;
      }
      playback.pause();
      setState(PlayerState.pause(state.getSong().orElseThrow(IllegalStateException::new)));
    } finally {
      stateLock.unlock();
    }
  }

  public void play() {
    Lock stateLock = this.stateLock;
    stateLock.lock();
    try {
      log.info("Playing...");
      if (getState().getState() == State.PLAY) {
        log.finer("Already playing.");
        return;
      }
      playback.play();
      setState(PlayerState.play(state.getSong().orElseThrow(IllegalStateException::new)));
    } finally {
      stateLock.unlock();
    }
  }

  public void next() throws InterruptedException {
    PlayerState state = getState();
    Lock stateLock = this.stateLock;
    stateLock.lock();
    try {
      log.info("Next...");
      PlayerState newState = getState();
      if (isSignificantlyDifferent(state, newState)) {
        log.fine("Skipping next call due to state change while waiting for lock.");
        return;
      }

      try {
        playback.close();
      } catch (Exception e) {
        log.severe("Error closing playback: " + e);
      }

      Song nextSong = queue.pop().orElseGet(suggester::suggestNext);
      songPlayedNotifier.accept(nextSong);
      log.info("Next song is: " + nextSong);
      try {
        playback = nextSong.getPlayback();
      } catch (IOException e) {
        log.severe("Error creating playback: " + e);

        setState(PlayerState.error());
        playback = DummyPlayback.getInstance();
        return;
      }
      setState(PlayerState.pause(nextSong));
      play();
    } finally {
      stateLock.unlock();
    }
  }

  private boolean isSignificantlyDifferent(PlayerState state, PlayerState other) {
    return state.hasSong() != state.hasSong()
        || other.hasSong() && state.hasSong() && !state.getSong().equals(other.getSong());
  }

  private void autoPlay() {
    try {
      PlayerState state = getState();
      playback.waitForFinish();

      Lock stateLock = this.stateLock;
      stateLock.lock();
      try {
        // Prevent auto next calls if next was manually called
        if (isSignificantlyDifferent(getState(), state)) {
          log.fine("Skipping auto call to next()");
        } else {
          log.fine("Auto call to next()");
          next();
        }
      } finally {
        stateLock.unlock();
      }

      executorService.submit(this::autoPlay);
    } catch (InterruptedException e) {
      log.fine("interrupted: " + e);
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
