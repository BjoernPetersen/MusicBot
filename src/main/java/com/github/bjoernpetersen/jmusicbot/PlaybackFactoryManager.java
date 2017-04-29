package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory;
import com.github.bjoernpetersen.jmusicbot.playback.included.DefaultWavePlaybackFactory;
import com.github.bjoernpetersen.jmusicbot.playback.included.WavePlaybackFactory;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public final class PlaybackFactoryManager implements Closeable {

  private static final Logger log = Logger.getLogger(PlaybackFactoryManager.class.getName());

  private final Map<Class<? extends PlaybackFactory>, PlaybackFactory> factories;

  PlaybackFactoryManager() {
    this.factories = new HashMap<>();
  }

  @Nonnull
  public <F extends PlaybackFactory> F getFactory(Class<F> factoryType) {
    PlaybackFactory result = factories.get(factoryType);
    if (result == null) {
      throw new IllegalArgumentException();
    } else {
      return (F) result;
    }
  }

  void loadFactories(File pluginFolder, Collection<PlaybackFactory> includedFactories) {
    factories.clear();
    factories.put(WavePlaybackFactory.class, new DefaultWavePlaybackFactory());

    for (PlaybackFactory factory : includedFactories) {
      storeFactoryForValidBases(factory);
    }

    Collection<PlaybackFactory> factories = new PluginLoader<>(
      pluginFolder,
      PlaybackFactory.class
    ).load();

    for (PlaybackFactory factory : factories) {
      storeFactoryForValidBases(factory);
    }
  }

  private void storeFactoryForValidBases(PlaybackFactory factory) {
    for (Class<? extends PlaybackFactory> base : factory.getBases()) {
      if (!base.isAssignableFrom(factory.getClass())) {
        log.severe(String.format("Bad base '%s' for PlaybackFactory: %s", base, factory));
        return;
      }
      try {
        factory.initialize();
      } catch (InitializationException e) {
        log.severe(String.format("Could not initialize PlaybackFactory '%s': %s", factory, e));
        continue;
      }
      factories.put(base, factory);
    }
  }

  @Override
  public void close() throws IOException {
    for (PlaybackFactory playbackFactory : factories.values()) {
      playbackFactory.close();
    }
    factories.clear();
  }
}
