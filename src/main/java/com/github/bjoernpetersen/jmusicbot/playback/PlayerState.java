package com.github.bjoernpetersen.jmusicbot.playback;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerState {

  @Nonnull
  private final State state;
  @Nullable
  private final SongEntry entry;

  public enum State {
    PLAY(true), PAUSE(true), STOP(false), ERROR(false);

    private final boolean hasSong;

    State(boolean hasSong) {
      this.hasSong = hasSong;
    }

    boolean hasSong() {
      return hasSong;
    }
  }

  private PlayerState(@Nonnull State state, @Nullable SongEntry entry) {
    this.state = Objects.requireNonNull(state);
    this.entry = entry;
  }

  @Nonnull
  public State getState() {
    return state;
  }

  @Nonnull
  public Optional<SongEntry> getEntry() {
    return Optional.ofNullable(entry);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PlayerState that = (PlayerState) o;

    if (state != that.state) {
      return false;
    }
    return entry != null ? entry.equals(that.entry) : that.entry == null;
  }

  @Override
  public int hashCode() {
    int result = state.hashCode();
    result = 31 * result + (entry != null ? entry.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "PlayerState{" +
        "state=" + state +
        ", entry=" + entry +
        '}';
  }

  @Nonnull
  static PlayerState play(SongEntry entry) {
    return new PlayerState(State.PLAY, Objects.requireNonNull(entry));
  }

  @Nonnull
  static PlayerState pause(SongEntry entry) {
    return new PlayerState(State.PAUSE, Objects.requireNonNull(entry));
  }

  @Nonnull
  static PlayerState stop() {
    return new PlayerState(State.STOP, null);
  }

  @Nonnull
  static PlayerState error() {
    return new PlayerState(State.ERROR, null);
  }
}
