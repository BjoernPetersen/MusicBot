package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.platform.Platform;
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory;
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactoryLoader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

public final class PlaybackFactoryManager implements Loggable, Closeable {

  @Nonnull
  private final Config config;
  private final Map<Class<? extends PlaybackFactory>, PlaybackFactory> factories;
  private final Map<PlaybackFactory, List<? extends Config.Entry>> configEntries;

  public PlaybackFactoryManager(@Nonnull Config config,
      @Nonnull Collection<PlaybackFactory> included) {
    this.config = config;
    this.factories = new HashMap<>();

    String pluginFolderName = config.getDefaults().getPluginFolder().getValue();
    File pluginFolder = new File(pluginFolderName);
    this.configEntries = loadFactories(pluginFolder, included);
  }

  /**
   * Gets a PlaybackFactory that implements the given marker interface.
   *
   * @param factoryType a PlaybackFactory marker interface
   * @param <F> the type of PlaybackFactory
   * @return an instance of F
   * @throws IllegalArgumentException if no implementation of the given interface is active.
   */
  @SuppressWarnings("unchecked")
  @Nonnull
  public <F extends PlaybackFactory> F getFactory(@Nonnull Class<F> factoryType) {
    PlaybackFactory result = factories.get(factoryType);
    if (result == null) {
      throw new IllegalArgumentException("No factory for: " + factoryType.getSimpleName());
    } else {
      try {
        return (F) result;
      } catch (ClassCastException e) {
        throw new IllegalArgumentException("Wrong type for factory: " + factoryType.getName(), e);
      }
    }
  }

  /**
   * Checks whether there is an active factory implementing the specified type.
   *
   * @param factoryType a PlaybackFactory marker interface
   * @return whether an implementing factory is active
   */
  public boolean hasFactory(@Nonnull Class<? extends PlaybackFactory> factoryType) {
    return factories.containsKey(factoryType);
  }

  @Nonnull
  public Collection<PlaybackFactory> getPlaybackFactories() {
    return Collections.unmodifiableCollection(configEntries.keySet());
  }

  @Nonnull
  public List<? extends Config.Entry> getConfigEntries(PlaybackFactory factory) {
    return configEntries.get(factory);
  }

  @Override
  public void close() throws IOException {
    for (PlaybackFactory playbackFactory : getPlaybackFactories()) {
      playbackFactory.close();
    }
    factories.clear();
  }

  @Nonnull
  private Map<PlaybackFactory, List<? extends Config.Entry>> loadFactories(
      @Nonnull File pluginFolder,
      @Nonnull Collection<PlaybackFactory> includedFactories) {
    Map<PlaybackFactory, List<? extends Config.Entry>> result = new HashMap<>();
    for (PlaybackFactory factory : includedFactories) {
      try {
        result.put(factory, storeFactoryForValidBases(factory));
      } catch (InvalidFactoryException | InitializationException e) {
        logSevere(e, "Could not load included factory " + factory.getReadableName());
      }
    }

    Collection<PlaybackFactory> factories = new PluginLoader<>(
        pluginFolder,
        PlaybackFactory.class
    ).load();

    for (PlaybackFactory factory : factories) {
      try {
        result.put(factory, storeFactoryForValidBases(factory));
      } catch (InvalidFactoryException | InitializationException e) {
        logInfo(e, "Could not load factory " + factory.getReadableName());
      }
    }

    Platform platform = Platform.get();
    for (PlaybackFactoryLoader loader : new PluginLoader<>(pluginFolder,
        PlaybackFactoryLoader.class).load()) {
      PlaybackFactory factory = loader.load(platform);
      if (factory == null) {
        logInfo("Platform not supported by PlaybackFactory %s", loader.getReadableName());
      } else {
        try {
          result.put(factory, storeFactoryForValidBases(factory));
        } catch (InvalidFactoryException | InitializationException e) {
          logInfo(e, "Could not load factory %s", factory.getReadableName());
        }
      }
    }

    return result;
  }

  @Nonnull
  private List<? extends Config.Entry> storeFactoryForValidBases(@Nonnull PlaybackFactory factory)
      throws InvalidFactoryException, InitializationException {
    List<Class<? extends PlaybackFactory>> validBases = new LinkedList<>();
    for (Class<? extends PlaybackFactory> base : factory.getBases()) {
      if (!base.isAssignableFrom(factory.getClass())) {
        logSevere("Bad base '%s' for PlaybackFactory: %s", base, factory);
        continue;
      }
      validBases.add(base);
    }

    if (validBases.isEmpty()) {
      throw new InvalidFactoryException();
    }

    List<? extends Config.Entry> result = factory.initializeConfigEntries(config);

    for (Class<? extends PlaybackFactory> base : validBases) {
      factories.put(base, factory);
    }

    return result;
  }

  void ensureConfigured(@Nonnull Configurator configurator) {
    List<PlaybackFactory> unconfigured = new LinkedList<>();
    for (PlaybackFactory factory : factories.values()) {
      List<? extends Config.Entry> missing;
      while (!(missing = factory.getMissingConfigEntries()).isEmpty()) {
        if (!configurator.configure(factory.getReadableName(), missing)) {
          logInfo("Deactivating unconfigured plugin " + factory.getReadableName());
          unconfigured.add(factory);
          break;
        }
      }
    }

    for (PlaybackFactory factory : unconfigured) {
      removeFactory(factory);
      factory.destructConfigEntries();
    }
  }

  void initializeFactories(@Nonnull InitStateWriter initStateWriter) throws InterruptedException {
    List<PlaybackFactory> defective = new LinkedList<>();
    for (PlaybackFactory factory : factories.values()) {
      try {
        initStateWriter.begin(factory.getReadableName());
        factory.initialize(initStateWriter);
      } catch (InitializationException e) {
        logWarning(e, "Could not initialize PlaybackFactory '%s'", factory);
        defective.add(factory);
      }
    }

    for (PlaybackFactory factory : defective) {
      removeFactory(factory);
      factory.destructConfigEntries();
    }
  }

  private void removeFactory(PlaybackFactory factory) {
    for (Class<? extends PlaybackFactory> base : factory.getBases()) {
      PlaybackFactory registered = getFactory(base);
      if (registered == factory) {
        factories.remove(base);
      }
    }
  }

  private static class InvalidFactoryException extends Exception {

  }
}
