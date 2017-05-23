package com.github.bjoernpetersen.jmusicbot.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.github.bjoernpetersen.jmusicbot.EqualsContract;
import com.github.bjoernpetersen.jmusicbot.Song;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PlayerStateTest implements EqualsContract<PlayerState> {

  private Song song;

  @Nonnull
  private static Song mockSong() {
    return mock(Song.class);
  }

  @Test
  void play() {
    Song song = mockSong();
    PlayerState state = PlayerState.play(song);

    assertSame(PlayerState.State.PLAY, state.getState());
    Optional<Song> songOptional = state.getSong();
    assertTrue(songOptional.isPresent());
    assertEquals(song, songOptional.get());
    assertSame(song, songOptional.get());
  }

  @Test
  void playNullSong() {
    assertThrows(NullPointerException.class, () -> PlayerState.play(null));
  }

  @Test
  void pause() {
    Song song = mockSong();
    PlayerState state = PlayerState.pause(song);

    assertSame(PlayerState.State.PAUSE, state.getState());
    Optional<Song> songOptional = state.getSong();
    assertTrue(songOptional.isPresent());
    assertEquals(song, songOptional.get());
    assertSame(song, songOptional.get());
  }

  @Test
  void pauseNullSong() {
    assertThrows(NullPointerException.class, () -> PlayerState.pause(null));
  }

  @Test
  void stop() {
    assertSame(PlayerState.State.STOP, PlayerState.stop().getState());
  }

  @Test
  void stopNoSong() {
    assertFalse(PlayerState.stop().getSong().isPresent());
  }

  @Test
  void error() {
    assertSame(PlayerState.State.ERROR, PlayerState.error().getState());
  }

  @Test
  void errorNoSong() {
    assertFalse(PlayerState.error().getSong().isPresent());
  }

  @SuppressWarnings("unused") // used as MethodSource
  private static List<PlayerState> getPossibleStates() {
    Song song = mockSong();
    return Arrays.asList(
        PlayerState.play(song), PlayerState.pause(song),
        PlayerState.stop(), PlayerState.error()
    );
  }

  @ParameterizedTest
  @MethodSource(names = "getPossibleStates")
  void getSongNotNull(PlayerState state) {
    assertNotNull(state.getSong());
  }

  @Nonnull
  @Override
  public PlayerState createValue() {
    song = mockSong();
    return PlayerState.play(song);
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
        return PlayerState.play(mockSong());
      case 1:
        return PlayerState.pause(song);
      case 2:
        return PlayerState.pause(mockSong());
      case 3:
        return PlayerState.stop();
      case 4:
        return PlayerState.error();
      default:
        throw new IllegalArgumentException();
    }
  }
}
