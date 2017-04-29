package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.api.RestApi;
import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.config.DefaultConfigEntry;
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory;
import com.github.bjoernpetersen.jmusicbot.playback.Player;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
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
    List<PlaybackFactory> playbackFactories) {
    this.config = config;
    File pluginFolder = new File(DefaultConfigEntry.get(config).pluginFolder.getOrDefault());

    this.playbackFactoryManager = new PlaybackFactoryManager();
    playbackFactoryManager.loadFactories(pluginFolder, playbackFactories);

    this.providerManager = new ProviderManager(
      config,
      playbackFactoryManager,
      providers,
      suggesters
    );

    Optional<String> primarySuggesterName = DefaultConfigEntry.get(config).suggester.get();
    if (!primarySuggesterName.isPresent()) {
      throw new IllegalStateException("Primary suggester undefined.");
    }
    Suggester primarySuggester;
    try {
      primarySuggester = providerManager.getSuggester(primarySuggesterName.get());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(String.format(
        "Primary suggester '%s' is missing.", primarySuggesterName.get()
      ));
    }

    Consumer<Song> songPlayedNotifier = song -> {
      Provider provider = providerManager.getProvider(song.getProviderName());
      for (Suggester suggester : providerManager.getSuggestersFor(provider)) {
        suggester.notifyPlayed(song);
      }
    };

    this.player = new Player(songPlayedNotifier, primarySuggester);
    this.restApi = new RestApi(this);
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
    @Nonnull
    private final List<PlaybackFactory> playbackFactories;

    public Builder(Config config) {
      // TODO register config entries
      this.config = config;
      this.providers = new LinkedList<>();
      this.suggesters = new LinkedList<>();
      this.playbackFactories = new LinkedList<>();
    }

    @Nonnull
    public Builder includePlaybackFactory(PlaybackFactory factory) {
      factory.initializeConfigEntries(config);
      this.playbackFactories.add(factory);
      return this;
    }

    @Nonnull
    public Builder addProvider(Provider provider) {
      provider.initializeConfigEntries(config);
      providers.add(provider);
      return this;
    }

    @Nonnull
    public Builder addSuggester(Suggester suggester) {
      suggester.initializeConfigEntries(config);
      suggesters.add(suggester);
      return this;
    }

    @Nonnull
    public MusicBot build() {
      return new MusicBot(config, providers, suggesters, playbackFactories);
    }

    @Override
    public String toString() {
      return "Builder{" +
        "providers=" + providers +
        ", suggesters=" + suggesters +
        ", playbackFactories=" + playbackFactories +
        '}';
    }
  }
}
