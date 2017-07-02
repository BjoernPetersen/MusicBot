package com.github.bjoernpetersen.jmusicbot;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SongBuilderTest {

  private Song.Builder builder;
  private PlaybackSupplier playbackSupplier;
  private SongLoader loader;
  private Provider provider;

  @BeforeEach
  void createBuilder() {
    builder = new Song.Builder();
  }

  @BeforeEach
  void createPlaybackSupplier() {
    playbackSupplier = song -> null;
  }

  @BeforeEach
  void createSongLoader() {
    loader = song -> false;
  }

  @BeforeEach
  void createProvider() {
    provider = new TestProvider();
  }

  @Test
  void idNullArg() {
    assertThrows(NullPointerException.class, () -> builder.id(null));
  }

  @Test
  void idValidArg() {
    builder.id("testId");
  }

  @Test
  void titleNullArg() {
    assertThrows(NullPointerException.class, () -> builder.title(null));
  }

  @Test
  void titleValidArg() {
    builder.title("testTitle");
  }

  @Test
  void descriptionNullArg() {
    assertThrows(NullPointerException.class, () -> builder.description(null));
  }

  @Test
  void descriptionValidArg() {
    builder.description("testDescription");
  }

  @Test
  void playbackSupplierNullArg() {
    assertThrows(NullPointerException.class, () -> builder.playbackSupplier(null));
  }

  @Test
  void playbackSupplierValidArg() {
    builder.playbackSupplier(playbackSupplier);
  }

  @Test
  void songLoaderNullArg() {
    assertThrows(NullPointerException.class, () -> builder.songLoader(null));
  }

  @Test
  void songLoaderValidArg() {
    builder.songLoader(loader);
  }

  @Test
  void providerNullArg() {
    assertThrows(NullPointerException.class, () -> builder.provider(null));
  }

  @Test
  void providerValidArg() {
    builder.provider(provider);
  }

  @Test
  void buildValid() {
    builder
        .playbackSupplier(playbackSupplier)
        .songLoader(loader)
        .provider(provider)
        .id("testId")
        .title("testTitle")
        .description("testDesc")
        .build();
  }

  @Test
  void buildMissingPlaybackSupplier() {
    assertThrows(IllegalStateException.class, () ->
        builder
            .songLoader(loader)
            .provider(provider)
            .id("testId")
            .title("testTitle")
            .description("testDesc")
            .build()
    );
  }

  @Test
  void buildMissingSongLoader() {
    assertThrows(IllegalStateException.class, () ->
        builder
            .playbackSupplier(playbackSupplier)
            .provider(provider)
            .id("testId")
            .title("testTitle")
            .description("testDesc")
            .build()
    );
  }

  @Test
  void buildMissingProvider() {
    assertThrows(IllegalStateException.class, () ->
        builder
            .playbackSupplier(playbackSupplier)
            .songLoader(loader)
            .id("testId")
            .title("testTitle")
            .description("testDesc")
            .build()
    );
  }

  @Test
  void buildMissingId() {
    assertThrows(IllegalStateException.class, () ->
        builder
            .playbackSupplier(playbackSupplier)
            .songLoader(loader)
            .provider(provider)
            .title("testTitle")
            .description("testDesc")
            .build()
    );
  }

  @Test
  void buildMissingTitle() {
    assertThrows(IllegalStateException.class, () ->
        builder
            .playbackSupplier(playbackSupplier)
            .songLoader(loader)
            .provider(provider)
            .id("testId")
            .description("testDesc")
            .build()
    );
  }

  @Test
  void buildMissingDescription() {
    assertThrows(IllegalStateException.class, () ->
        builder
            .playbackSupplier(playbackSupplier)
            .songLoader(loader)
            .provider(provider)
            .id("testId")
            .title("testTitle")
            .build()
    );
  }

  @AfterAll
  static void resetSongLoader() {
    SongLoaderExecutor.getInstance().close();
  }
}
