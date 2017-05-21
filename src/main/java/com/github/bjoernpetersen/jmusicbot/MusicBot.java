package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.api.RestApi;
import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.playback.Player;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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

  private MusicBot(Config config, List<Provider> providers, List<Suggester> suggesters,
      PlaybackFactoryManager playbackFactoryManager, @Nullable Suggester defaultSuggester)
      throws InitializationException {
    try {
      this.config = config;
      this.playbackFactoryManager = playbackFactoryManager;

      this.providerManager = new ProviderManager(
          config,
          playbackFactoryManager,
          providers,
          suggesters
      );

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
    @Nonnull
    private final List<Provider> providers;
    @Nonnull
    private final List<Suggester> suggesters;
    @Nullable
    private PlaybackFactoryManager playbackFactoryManager;
    @Nullable
    private Suggester defaultSuggester;

    public Builder(Config config) {
      this.config = config;
      this.providers = new LinkedList<>();
      this.suggesters = new LinkedList<>();
    }


    @Nonnull
    public Builder playbackFactoryManager(PlaybackFactoryManager manager) {
      this.playbackFactoryManager = manager;
      return this;
    }

    /**
     * Adds the specified provider to the list of providers and initializes its config entries.
     *
     * @param provider a provider
     * @return this Builder
     */
    @Nonnull
    public Builder addProvider(Provider provider) {
      provider.initializeConfigEntries(config);
      providers.add(provider);
      return this;
    }

    /**
     * Adds the specified suggester to the list of providers and initializes its config entries.
     *
     * @param suggester a suggester
     * @return this Builder
     */
    @Nonnull
    public Builder addSuggester(Suggester suggester) {
      suggester.initializeConfigEntries(config);
      suggesters.add(suggester);
      return this;
    }

    @Nonnull
    public Builder defaultSuggester(@Nullable Suggester suggester) {
      this.defaultSuggester = suggester;
      return this;
    }

    @Nonnull
    public MusicBot build() throws InitializationException {
      return new MusicBot(config, providers, suggesters, playbackFactoryManager, defaultSuggester);
    }

    @Override
    public String toString() {
      return "Builder{"
          + "providers=" + providers
          + ", suggesters=" + suggesters
          + '}';
    }
  }
}
