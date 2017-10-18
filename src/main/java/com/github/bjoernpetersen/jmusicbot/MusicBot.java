package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.Plugin.State;
import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.playback.Player;
import com.github.bjoernpetersen.jmusicbot.playback.QueueChangeListener;
import com.github.bjoernpetersen.jmusicbot.playback.QueueEntry;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.github.bjoernpetersen.jmusicbot.provider.ProviderManager;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import com.github.bjoernpetersen.jmusicbot.user.UserManager;
import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import com.google.common.annotations.VisibleForTesting;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Nonnull
public final class MusicBot implements Loggable, Closeable {

  private static final int PORT = 42945;
  private static final String GROUP_ADDRESS = "224.0.0.142";

  private final Config config;
  private final Player player;
  private final PlaybackFactoryManager playbackFactoryManager;
  private final ProviderManager providerManager;
  private final UserManager userManager;
  private final Closeable restApi;
  private final Closeable broadcaster;
  private final Set<AdminPlugin> adminPlugins;

  private MusicBot(@Nonnull Config config, @Nonnull InitStateWriter initStateWriter,
      @Nonnull PlaybackFactoryManager playbackFactoryManager,
      @Nonnull ProviderManager providerManager, @Nullable Suggester defaultSuggester,
      @Nonnull UserManager userManager, @Nonnull ApiInitializer apiInitializer,
      @Nonnull BroadcasterInitializer broadcasterInitializer,
      @Nonnull Set<AdminPlugin> adminPlugins)
      throws InitializationException {
    this.config = config;
    this.playbackFactoryManager = playbackFactoryManager;
    this.providerManager = providerManager;
    this.userManager = userManager;

    List<Closeable> initialized = new ArrayList<>();
    initialized.add(playbackFactoryManager);
    initialized.add(providerManager);
    initialized.add(userManager);

    if (defaultSuggester != null
        && providerManager.getWrapper(defaultSuggester).getState() != State.ACTIVE) {
      initStateWriter.warning("Default suggester is not active.");
      defaultSuggester = null;
    }

    try {
      checkSanity(providerManager);
    } catch (InitializationException e) {
      closeAll(e, initialized);
      throw e;
    }

    Consumer<Song> songPlayedNotifier = song -> {
      Provider provider = song.getProvider();
      for (Suggester suggester : providerManager.getSuggesters(provider)) {
        suggester.notifyPlayed(song);
      }
    };

    try {
      this.player = new Player(songPlayedNotifier, defaultSuggester);
      initialized.add(this.player);
    } catch (RuntimeException e) {
      closeAll(e, initialized);
      throw new InitializationException("Exception during player init", e);
    }
    this.player.getQueue().addListener(new QueueChangeListener() {
      @Override
      public void onAdd(@Nonnull QueueEntry entry) {
        Song song = entry.getSong();
        Provider provider = song.getProvider();
        for (Suggester suggester : providerManager.getSuggesters(provider)) {
          suggester.removeSuggestion(song);
        }
      }

      @Override
      public void onRemove(@Nonnull QueueEntry entry) {
      }
    });

    try {
      this.restApi = apiInitializer.initialize(this, PORT);
      initialized.add(this.restApi);
    } catch (InitializationException e) {
      closeAll(e, initialized);
      throw new InitializationException("Exception during REST init", e);
    }

    try {
      this.broadcaster = broadcasterInitializer.initialize(
          PORT,
          GROUP_ADDRESS,
          PORT + ";1.0"
      );
      initialized.add(this.broadcaster);
    } catch (InitializationException e) {
      closeAll(e, initialized);
      throw new InitializationException("Exception during broadcaster init", e);
    }

    this.adminPlugins = adminPlugins;
    try {
      for (AdminPlugin plugin : adminPlugins) {
        plugin.initialize(initStateWriter, this);
        initialized.add(plugin);
      }
    } catch (InitializationException e) {
      closeAll(e, initialized);
      throw new InitializationException("Exception during admin plugin init", e);
    }
  }

  private void checkSanity(@Nonnull ProviderManager providerManager)
      throws InitializationException {
    if (providerManager.getProviders().count() == 0) {
      throw new InitializationException("There are no active providers. This setup is useless.");
    }
  }

  private void closeAll(Exception cause, List<Closeable> toClose) {
    for (Closeable closeable : toClose) {
      try {
        closeable.close();
      } catch (IOException e) {
        cause.addSuppressed(e);
      }
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

  @Nullable
  private IOException tryClose(ExceptionThrower closer, @Nullable IOException e) {
    try {
      closer.close();
    } catch (Throwable throwable) {
      if (e == null) {
        return new IOException(throwable);
      } else {
        e.addSuppressed(throwable);
      }
    }
    return e;
  }

  @FunctionalInterface
  private interface ExceptionThrower<E extends Throwable> {

    void close() throws E;
  }

  @Override
  public void close() throws IOException {
    IOException e = tryClose(broadcaster::close, null);
    for (AdminPlugin adminPlugin : adminPlugins) {
      e = tryClose(adminPlugin::close, e);
    }
    e = tryClose(restApi::close, e);
    e = tryClose(userManager::close, e);
    e = tryClose(player::close, e);
    e = tryClose(getProviderManager()::close, e);
    e = tryClose(getPlaybackFactoryManager()::close, e);
    e = tryClose(SongLoaderExecutor.getInstance()::close, e);
    e = tryClose(PluginLoader::reset, e);
    if (e != null) {
      throw e;
    }
  }

  /**
   * Gets the version of this MusicBot.
   *
   * @return a version
   */
  @Nonnull
  public static Version getVersion() {
    try {
      Properties properties = new Properties();
      properties.load(MusicBot.class.getResourceAsStream("version.properties"));
      String version = properties.getProperty("version");
      if (version == null) {
        throw new IllegalStateException("Version is missing");
      }
      return Version.valueOf(Version.valueOf(version).getNormalVersion());
    } catch (IOException | ParseException e) {
      throw new IllegalStateException("Could not read version resource", e);
    }
  }

  public static class Builder implements Loggable {

    // TODO IP, port
    @Nonnull
    private final Config config;
    @Nullable
    private Configurator configurator;
    @Nullable
    private ProviderManager providerManager;
    @Nullable
    private PlaybackFactoryManager playbackFactoryManager;
    @Nullable
    private Suggester defaultSuggester;
    @Nullable
    private ApiInitializer apiInitializer;
    @Nullable
    private BroadcasterInitializer broadcasterInitializer;
    @Nullable
    private UserManager userManager;
    @Nonnull
    private InitStateWriter initStateWriter;
    @Nonnull
    private Set<AdminPlugin> adminPlugins;

    public Builder(@Nonnull Config config) {
      this.config = config;
      this.initStateWriter = InitStateWriter.NO_OP;
      this.adminPlugins = new HashSet<>();
    }

    @Nonnull
    public Builder configurator(@Nonnull Configurator configurator) {
      this.configurator = Objects.requireNonNull(configurator);
      return this;
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
    public Builder broadcasterInitializer(@Nonnull BroadcasterInitializer broadcasterInitializer) {
      this.broadcasterInitializer = Objects.requireNonNull(broadcasterInitializer);
      return this;
    }

    @Nonnull
    public Builder initStateWriter(@Nonnull InitStateWriter initStateWriter) {
      this.initStateWriter = Objects.requireNonNull(initStateWriter);
      return this;
    }

    @Nonnull
    public Builder addAdminPlugin(@Nonnull AdminPlugin adminPlugin) {
      this.adminPlugins.add(Objects.requireNonNull(adminPlugin));
      return this;
    }

    private Configurator.Result ensureConfigured(@Nonnull Configurator configurator,
        @Nonnull AdminPlugin plugin) {
      List<? extends Config.Entry> missing;
      while (!(missing = plugin.getMissingConfigEntries()).isEmpty()) {
        Configurator.Result result = configurator.configure(plugin.getReadableName(), missing);
        if (!result.equals(Configurator.Result.OK)) {
          return result;
        }
      }
      return Configurator.Result.OK;
    }

    private void ensureConfigured(@Nonnull Configurator configurator,
        @Nonnull Set<AdminPlugin> plugins) throws CancelException {
      Iterator<AdminPlugin> iterator = plugins.iterator();
      while (iterator.hasNext()) {
        AdminPlugin plugin = iterator.next();
        Configurator.Result result = ensureConfigured(configurator, plugin);
        switch (result) {
          case CANCEL:
            throw new CancelException("User cancelled config");
          case DISABLE:
            plugin.destructConfigEntries();
            iterator.remove();
          case OK:
          default:
            // just continue
        }
      }
    }

    @Nonnull
    public MusicBot build() throws CancelException, InitializationException, InterruptedException {
      if (configurator == null
          || providerManager == null
          || playbackFactoryManager == null
          || userManager == null
          || apiInitializer == null
          || broadcasterInitializer == null) {
        throw new IllegalStateException("Not all required values set.");
      }

      try {
        printUnsupported("PlaybackFactories", playbackFactoryManager.getPlaybackFactories());
        printUnsupported("Providers", providerManager.getAllProviders().values());
        printUnsupported("Suggesters", providerManager.getAllProviders().values());

        playbackFactoryManager.ensureConfigured(configurator);
        providerManager.ensureProvidersConfigured(configurator);
        providerManager.ensureSuggestersConfigured(configurator);
        ensureConfigured(configurator, adminPlugins);

        playbackFactoryManager.initializeFactories(initStateWriter);
        providerManager.initializeProviders(initStateWriter);
        providerManager.initializeSuggesters(initStateWriter);

        return new MusicBot(
            config,
            initStateWriter,
            playbackFactoryManager,
            providerManager,
            defaultSuggester,
            userManager,
            apiInitializer,
            broadcasterInitializer,
            adminPlugins
        );
      } finally {
        initStateWriter.close();
      }
    }

    private void printUnsupported(@Nonnull String kind,
        @Nonnull Collection<? extends Plugin> plugins) {
      String message = getUnsupportedMessage(kind, plugins);
      if (message != null) {
        logWarning(message);
      }
    }

    @VisibleForTesting
    @Nullable
    String getUnsupportedMessage(@Nonnull String kind,
        @Nonnull Collection<? extends Plugin> plugins) {
      StringJoiner stringJoiner = new StringJoiner(", ");
      plugins.stream()
          .filter(this::isUnsupported)
          .map(Plugin::getReadableName)
          .forEach(stringJoiner::add);

      String unsupported = stringJoiner.toString();
      if (unsupported.isEmpty()) {
        return null;
      }

      return String.format("The following %s are probably not supported: %s", kind, unsupported);
    }

    @VisibleForTesting
    boolean isUnsupported(@Nonnull Plugin plugin) {
      Version currentVersion = MusicBot.getVersion();
      Version minVersion = plugin.getMinSupportedVersion();
      Version maxVersion = plugin.getMaxSupportedVersion();
      return isUnsupported(currentVersion, minVersion, maxVersion);
    }

    @VisibleForTesting
    boolean isUnsupported(@Nonnull Version current,
        @Nonnull Version min, @Nonnull Version max) {
      if (current.getMajorVersion() != min.getMajorVersion()) {
        if (current.getMajorVersion() < min.getMajorVersion()) {
          return true;
        }
      } else if (current.getMinorVersion() < min.getMinorVersion()) {
        return true;
      }

      if (current.getMajorVersion() != max.getMajorVersion()) {
        if (current.getMajorVersion() > max.getMajorVersion()) {
          return true;
        }
      } else if (current.getMinorVersion() > max.getMinorVersion()) {
        return true;
      }
      return false;
    }

  }
}
