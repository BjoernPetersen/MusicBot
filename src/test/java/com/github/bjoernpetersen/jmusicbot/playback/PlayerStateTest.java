package com.github.bjoernpetersen.jmusicbot.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.github.bjoernpetersen.jmusicbot.EqualsContract;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class PlayerStateTest implements EqualsContract<PlayerState> {

  private SongEntry songEntry;

  @Nonnull
  private static SongEntry mockEntry() {
    return mock(SongEntry.class);
  }

  @Test
  void play() {
    SongEntry entry = mockEntry();
    PlayerState state = new PlayState(entry);
    assertSame(entry, state.getEntry());
    assertTrue(state.hasSong());
  }

  @Test
  void playNullSong() {
    assertThrows(RuntimeException.class, () -> new PlayState(null));
  }

  @Test
  void pause() {
    SongEntry song = mockEntry();
    PlayerState state = new PauseState(song);
    assertSame(song, state.getEntry());
    assertTrue(state.hasSong());
  }

  @Test
  void pauseNullSong() {
    assertThrows(RuntimeException.class, () -> new PauseState(null));
  }

  @Test
  void stop() {
    PlayerState state = new StopState();
    assertNull(state.getEntry());
    assertFalse(state.hasSong());
    assertEquals(state.getClass().getSimpleName(), state.toString());
  }

  @Test
  void error() {
    PlayerState state = new ErrorState();
    assertNull(state.getEntry());
    assertFalse(state.hasSong());
    assertEquals(state.getClass().getSimpleName(), state.toString());
  }

  @Nonnull
  @Override
  public PlayerState createValue() {
    songEntry = mockEntry();
    return new PlayState(songEntry);
  }

  @Override
  public int getEqualRelevantValueCount() {
    return 5;
  }

  @Nonnull
  @Override
  public PlayerState createNotEqualValue(int valueIndex) {
    switch (valueIndex) {
      case 0:
        return new PlayState(mockEntry());
      case 1:
        return new PauseState(songEntry);
      case 2:
        return new PauseState(mockEntry());
      case 3:
        return new StopState();
      case 4:
        return new ErrorState();
      default:
        throw new IllegalArgumentException();
    }
  }
}
