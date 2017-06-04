package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.ProviderManager.State;
import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.playback.Player;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Nonnull
public final class MusicBot implements Closeable {

  private static final Logger log = Logger.getLogger(MusicBot.class.getName());

  private final Config config;
  private final Player player;
  private final PlaybackFactoryManager playbackFactoryManager;
  private final ProviderManager providerManager;
  private final Closeable restApi;

  private MusicBot(@Nonnull Config config, @Nonnull PlaybackFactoryManager playbackFactoryManager,
      @Nonnull ProviderManager providerManager, @Nullable Suggester defaultSuggester,
      @Nonnull ApiInitializer apiInitializer)
      throws InitializationException {
    this.config = config;
    this.playbackFactoryManager = playbackFactoryManager;
    this.providerManager = providerManager;

    Consumer<Song> songPlayedNotifier = song -> {
      Provider provider = providerManager.getProvider(song.getProviderName());
      for (Suggester suggester : providerManager.getSuggestersFor(provider)) {
        suggester.notifyPlayed(song);
      }
    };

    if (defaultSuggester != null && providerManager.getState(defaultSuggester) != State.ACTIVE) {
      log.warning("Default suggester is not active.");
      defaultSuggester = null;
    }

    try {
      this.player = new Player(songPlayedNotifier, defaultSuggester);
    } catch (RuntimeException e) {
      throw new InitializationException("Exception during player init", e);
    }

    try {
      this.restApi = apiInitializer.initialize(this);
    } catch (InitializationException e) {
      try {
        player.close();
      } catch (IOException closeException) {
        log.severe("Tried to close player due to REST init error and got another exception: " + e);
      }
      throw new InitializationException("Exception during REST init", e);
    }
  }

  @Nonnull
  public Player getPlayer() {
    return player;
  }

  @Nonnull
  public PlaybackFactoryManager getPlaybackFactoryManager() {
    return playbackFactoryManager;
  }

  @Nonnull
  public ProviderManager getProviderManager() {
    return providerManager;
  }

  @Override
  public void close() throws IOException {
    restApi.close();
    player.close();
    getProviderManager().close();
    getPlaybackFactoryManager().close();
    SongLoader.reset();
    PluginLoader.reset();
  }

  public static class Builder {

    // TODO IP, port
    @Nonnull
    private final Config config;
    @Nullable
    private ProviderManager providerManager;
    @Nullable
    private PlaybackFactoryManager playbackFactoryManager;
    @Nullable
    private Suggester defaultSuggester;
    @Nullable
    private ApiInitializer apiInitializer;
    @Nonnull
    private InitStateWriter initStateWriter;

    public Builder(@Nonnull Config config) {
      this.config = config;
      this.initStateWriter = InitStateWriter.NO_OP;
    }

    @Nonnull
    public Builder playbackFactoryManager(@Nonnull PlaybackFactoryManager manager) {
      this.playbackFactoryManager = manager;
      return this;
    }

    @Nonnull
    public Builder providerManager(@Nonnull ProviderManager manager) {
      this.providerManager = manager;
      return this;
    }

    @Nonnull
    public Builder defaultSuggester(@Nullable Suggester suggester) {
      this.defaultSuggester = suggester;
      return this;
    }

    @Nonnull
    public Builder apiInitializer(@Nonnull ApiInitializer apiInitializer) {
      this.apiInitializer = Objects.requireNonNull(apiInitializer);
      return this;
    }

    @Nonnull
    public Builder initStateWriter(@Nonnull InitStateWriter initStateWriter) {
      this.initStateWriter = Objects.requireNonNull(initStateWriter);
      return this;
    }

    @Nonnull
    public MusicBot build() throws InitializationException, InterruptedException {
      if (providerManager == null
          || playbackFactoryManager == null
          || apiInitializer == null) {
        throw new IllegalStateException("ProviderManager or PlaybackFactoryManager is null");
      }

      playbackFactoryManager.initializeFactories(initStateWriter);

      for (Provider provider : providerManager.getProviders().values()) {
        if (providerManager.getState(provider) == State.CONFIG) {
          try {
            providerManager.initialize(provider, initStateWriter);
          } catch (InitializationException e) {
            log.severe(String.format(
                "Could not initialize Provider '%s': %s",
                provider.getReadableName(),
                e
            ));
            providerManager.destructConfigEntries(provider);
          }
        }
      }

      for (Suggester suggester : providerManager.getSuggesters().values()) {
        if (providerManager.getState(suggester) == State.CONFIG) {
          try {
            providerManager.initialize(suggester, initStateWriter);
          } catch (InitializationException e) {
            log.severe(String.format(
                "Could not initialize Suggester '%s': %s",
                suggester.getReadableName(),
                e
            ));
            providerManager.destructConfigEntries(suggester);
          }
        }
      }

      initStateWriter.close();

      return new MusicBot(
          config,
          playbackFactoryManager,
          providerManager,
          defaultSuggester,
          apiInitializer
      );
    }
  }
}
