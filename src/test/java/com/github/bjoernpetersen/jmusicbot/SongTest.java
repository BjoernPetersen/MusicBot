package com.github.bjoernpetersen.jmusicbot;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.bjoernpetersen.jmusicbot.Song.Builder;
import com.github.bjoernpetersen.jmusicbot.playback.AbstractPlayback;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SongTest implements EqualsContract<Song> {

  private Song song;

  @BeforeEach
  void initSong() {
    song = createValue();
  }

  @Test
  void getIdNonnull() {
    assertNotNull(song.getId());
  }

  @Test
  void getTitleNonnull() {
    assertNotNull(song.getTitle());
  }

  @Test
  void getDescriptionNonnull() {
    assertNotNull(song.getDescription());
  }

  @Test
  void getProviderNameNonnull() {
    assertNotNull(song.getProviderName());
  }

  @Test
  void hasLoadedNotLoading() {
    assertThrows(
        IllegalStateException.class,
        () -> assertTimeoutPreemptively(ofMillis(500), () -> song.hasLoaded())
    );
  }

  @Test
  void hasLoadedSuccessful() throws InterruptedException {
    song.load();
    assertTrue(assertTimeoutPreemptively(ofMillis(500), song::hasLoaded));
  }

  @Test
  void hasLoadedUnsuccessful() throws InterruptedException {
    Song song = filledBuilder().songLoader(new SongLoader() {
      @Override
      protected boolean loadImpl(@Nonnull Song song) {
        return false;
      }
    }).build();

    song.load();
    assertFalse(assertTimeoutPreemptively(ofMillis(500), song::hasLoaded));
  }

  @Test
  void getPlaybackSuccessful() {
    assertNotNull(assertTimeoutPreemptively(ofMillis(500), song::getPlayback));
    assertTrue(assertTimeoutPreemptively(ofMillis(500), song::hasLoaded));
  }

  @Test
  void getPlaybackUnsuccessful() {
    Song song = filledBuilder()
        .songLoader(new SongLoader() {
          @Override
          protected boolean loadImpl(@Nonnull Song song) {
            return false;
          }
        })
        .build();

    assertTimeoutPreemptively(ofMillis(500), () ->
        assertThrows(IOException.class, song::getPlayback));
    assertFalse(assertTimeoutPreemptively(ofMillis(500), song::hasLoaded));
  }

  private Builder filledBuilder() {
    return new Song.Builder()
        .playbackSupplier(s -> new AbstractPlayback() {
          @Override
          public void play() {
          }

          @Override
          public void pause() {
          }
        })
        .songLoader(new SongLoader() {
          @Override
          protected boolean loadImpl(@Nonnull Song song) {
            return true;
          }
        })
        .provider(new TestProvider())
        .id("test")
        .description("test")
        .title("test");
  }

  @Nonnull
  @Override
  public Song createValue() {
    return filledBuilder()
        .build();
  }

  @Override
  public int getEqualRelevantValueCount() {
    return 1;
  }

  @Nonnull
  @Override
  public Song createNotEqualValue(int valueIndex) {
    Song.Builder builder = filledBuilder();
    switch (valueIndex) {
      case 0:
        builder.id("test2");
        break;
      default:
        throw new IllegalArgumentException();
    }
    return builder.build();
  }
}
