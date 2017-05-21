package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.config.DefaultConfigEntry;
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public final class PlaybackFactoryManager implements Closeable {

  private static final Logger log = Logger.getLogger(PlaybackFactoryManager.class.getName());

  @Nonnull
  private final Config config;
  private final Map<Class<? extends PlaybackFactory>, PlaybackFactory> factories;
  private final Map<PlaybackFactory, List<? extends Config.Entry>> configEntries;

  public PlaybackFactoryManager(@Nonnull Config config,
      @Nonnull Collection<PlaybackFactory> included) {
    this.config = config;
    this.factories = new HashMap<>();

    String pluginFolderName = DefaultConfigEntry.get(config).pluginFolder.getOrDefault();
    File pluginFolder = new File(pluginFolderName);
    this.configEntries = loadFactories(pluginFolder, included);
  }

  @Nonnull
  public <F extends PlaybackFactory> F getFactory(@Nonnull Class<F> factoryType) {
    PlaybackFactory result = factories.get(factoryType);
    if (result == null) {
      throw new IllegalArgumentException();
    } else {
      return (F) result;
    }
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
        log.severe("Could not load included factory " + factory.toString() + ": " + e);
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
        log.severe("Could not load factory " + factory.toString() + ": " + e);
      }
    }

    return result;
  }

  @Nonnull
  private List<? extends Config.Entry> storeFactoryForValidBases(PlaybackFactory factory)
      throws InvalidFactoryException, InitializationException {
    List<Class<? extends PlaybackFactory>> validBases = new LinkedList<>();
    for (Class<? extends PlaybackFactory> base : factory.getBases()) {
      if (!base.isAssignableFrom(factory.getClass())) {
        log.severe(String.format("Bad base '%s' for PlaybackFactory: %s", base, factory));
        continue;
      }
      validBases.add(base);
    }

    if (validBases.isEmpty()) {
      throw new InvalidFactoryException();
    }

    List<? extends Config.Entry> result;
    try {
      result = factory.initializeConfigEntries(config);
      factory.initialize();
    } catch (InitializationException e) {
      log.severe(String.format("Could not initialize PlaybackFactory '%s': %s", factory, e));
      throw new InitializationException(e);
    }

    for (Class<? extends PlaybackFactory> base : validBases) {
      factories.put(base, factory);
    }

    return result;
  }

  private static class InvalidFactoryException extends Exception {

  }
}
