package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.ProviderManager.State;
import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.playback.Player;
import com.github.bjoernpetersen.jmusicbot.playback.Queue;
import com.github.bjoernpetersen.jmusicbot.playback.QueueChangeListener;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import com.github.bjoernpetersen.jmusicbot.user.UserManager;
import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Nonnull
public final class MusicBot implements Loggable, Closeable {

  private final Config config;
  private final Player player;
  private final PlaybackFactoryManager playbackFactoryManager;
  private final ProviderManager providerManager;
  private final UserManager userManager;
  private final Closeable restApi;

  private MusicBot(@Nonnull Config config, @Nonnull PlaybackFactoryManager playbackFactoryManager,
      @Nonnull ProviderManager providerManager, @Nullable Suggester defaultSuggester,
      @Nonnull UserManager userManager, @Nonnull ApiInitializer apiInitializer)
      throws InitializationException {
    this.config = config;
    this.playbackFactoryManager = playbackFactoryManager;
    this.providerManager = providerManager;
    this.userManager = userManager;

    Consumer<Song> songPlayedNotifier = song -> {
      Provider provider = providerManager.getProvider(song.getProviderName());
      for (Suggester suggester : providerManager.getSuggestersFor(provider)) {
        suggester.notifyPlayed(song);
      }
    };

    if (defaultSuggester != null && providerManager.getState(defaultSuggester) != State.ACTIVE) {
      logWarning("Default suggester is not active.");
      defaultSuggester = null;
    }

    try {
      this.player = new Player(songPlayedNotifier, defaultSuggester);
      this.player.getQueue().addListener(new QueueChangeListener() {
        @Override
        public void onAdd(@Nonnull Queue.Entry entry) {
          Song song = entry.getSong();
          Provider provider = providerManager.getProvider(song.getProviderName());
          for (Suggester suggester : providerManager.getSuggestersFor(provider)) {
            suggester.removeSuggestion(song);
          }
        }

        @Override
        public void onRemove(@Nonnull Queue.Entry entry) {
        }
      });
    } catch (RuntimeException e) {
      throw new InitializationException("Exception during player init", e);
    }

    try {
      this.restApi = apiInitializer.initialize(this);
    } catch (InitializationException e) {
      try {
        player.close();
      } catch (IOException closeException) {
        logSevere(e, "Tried to close player due to REST init error and got another exception: ");
        e.addSuppressed(e);
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

  @Nonnull
  public UserManager getUserManager() {
    return userManager;
  }

  @Override
  public void close() throws IOException {
    restApi.close();
    userManager.close();
    player.close();
    getProviderManager().close();
    getPlaybackFactoryManager().close();
    SongLoader.reset();
    PluginLoader.reset();
  }

  public static class Builder implements Loggable {

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
    @Nullable
    private UserManager userManager;
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
    public Builder userManager(@Nonnull UserManager userManager) {
      this.userManager = Objects.requireNonNull(userManager);
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
          || userManager == null
          || apiInitializer == null) {
        throw new IllegalStateException("ProviderManager or PlaybackFactoryManager is null");
      }

      playbackFactoryManager.initializeFactories(initStateWriter);

      for (Provider provider : providerManager.getProviders().values()) {
        if (providerManager.getState(provider) == State.CONFIG) {
          try {
            providerManager.initialize(provider, initStateWriter);
          } catch (InitializationException e) {
            logSevere(e, "Could not initialize Provider " + provider.getReadableName());
            providerManager.destructConfigEntries(provider);
          }
        }
      }

      for (Suggester suggester : providerManager.getSuggesters().values()) {
        if (providerManager.getState(suggester) == State.CONFIG) {
          try {
            providerManager.initialize(suggester, initStateWriter);
          } catch (InitializationException e) {
            logSevere(e, "Could not initialize Suggester " + suggester.getReadableName());
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
          userManager,
          apiInitializer
      );
    }
  }
}
