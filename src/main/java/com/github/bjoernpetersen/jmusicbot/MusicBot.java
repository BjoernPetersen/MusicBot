package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.ProviderManager.State;
import com.github.bjoernpetersen.jmusicbot.api.RestApi;
import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.playback.Player;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@Nonnull
public final class MusicBot implements Closeable {

  private static final Logger log = Logger.getLogger(MusicBot.class.getName());

  private final Config config;
  private final Player player;
  private final PlaybackFactoryManager playbackFactoryManager;
  private final ProviderManager providerManager;
  private final RestApi restApi;

  private MusicBot(Config config, PlaybackFactoryManager playbackFactoryManager,
      ProviderManager providerManager, @Nullable Suggester defaultSuggester)
      throws InitializationException {
    try {
      this.config = config;
      this.playbackFactoryManager = playbackFactoryManager;
      this.providerManager = providerManager;

      Consumer<Song> songPlayedNotifier = song -> {
        Provider provider = providerManager.getProvider(song.getProviderName());
        for (Suggester suggester : providerManager.getSuggestersFor(provider)) {
          suggester.notifyPlayed(song);
        }
      };

      this.player = new Player(songPlayedNotifier, defaultSuggester);
      this.restApi = new RestApi(this);
    } catch (Exception e) {
      log.severe("Exception during MusicBot initialization: " + e);
      throw new InitializationException(e);
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

  @ParametersAreNonnullByDefault
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

    public Builder(Config config) {
      this.config = config;
    }

    @Nonnull
    public Builder playbackFactoryManager(PlaybackFactoryManager manager) {
      this.playbackFactoryManager = manager;
      return this;
    }

    @Nonnull
    public Builder providerManager(ProviderManager manager) {
      this.providerManager = manager;
      return this;
    }

    @Nonnull
    public Builder defaultSuggester(@Nullable Suggester suggester) {
      this.defaultSuggester = suggester;
      return this;
    }

    @Nonnull
    public MusicBot build() throws InitializationException {
      if (providerManager == null
          || playbackFactoryManager == null) {
        throw new IllegalStateException("ProviderManager or PlaybackFactoryManager is null");
      }

      playbackFactoryManager.initializeFactories();

      for (Provider provider : providerManager.getProviders().values()) {
        if (providerManager.getState(provider) == State.CONFIG) {
          providerManager.initialize(provider);
        }
      }

      for (Suggester suggester : providerManager.getSuggesters().values()) {
        if (providerManager.getState(suggester) == State.CONFIG) {
          providerManager.initialize(suggester);
        }
      }

      return new MusicBot(config, playbackFactoryManager, providerManager, defaultSuggester);
    }
  }
}
