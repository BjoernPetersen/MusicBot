package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.Song;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerState {

  @Nonnull
  private final State state;
  @Nullable
  private final Song song;

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

  private PlayerState(@Nonnull State state, @Nullable Song song) {
    this.state = state;
    this.song = song;
  }

  @Nonnull
  public State getState() {
    return state;
  }

  public boolean hasSong() {
    return getState().hasSong();
  }

  @Nonnull
  public Optional<Song> getSong() {
    return Optional.ofNullable(song);
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
    return song != null ? song.equals(that.song) : that.song == null;
  }

  @Override
  public int hashCode() {
    int result = state.hashCode();
    result = 31 * result + (song != null ? song.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "PlayerState{" +
        "state=" + state +
        ", song=" + song +
        '}';
  }

  @Nonnull
  static PlayerState play(Song song) {
    return new PlayerState(State.PLAY, song);
  }

  @Nonnull
  static PlayerState pause(Song song) {
    return new PlayerState(State.PAUSE, song);
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
