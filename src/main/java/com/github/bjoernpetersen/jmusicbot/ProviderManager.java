package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.config.DefaultConfigEntry;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

public final class ProviderManager implements Closeable {

  @Nonnull
  private static final Logger log = Logger.getLogger(ProviderManager.class.getName());

  public enum State {
    INACTIVE, CONFIG, ACTIVE
  }

  @Nonnull
  private final Config config;
  @Nonnull
  private final PlaybackFactoryManager playbackFactoryManager;

  @Nonnull
  private final PluginManager<Provider> providerManager;
  @Nonnull
  private final PluginManager<Suggester> suggesterManager;

  @Nonnull
  private final Map<Provider, List<Suggester>> suggestersForProvider;

  public ProviderManager(@Nonnull Config config,
      @Nonnull PlaybackFactoryManager playbackFactoryManager) {
    this.config = config;
    this.playbackFactoryManager = playbackFactoryManager;

    this.providerManager = new PluginManager<>(
        config,
        Provider.class,
        p -> p.initialize(playbackFactoryManager)
    );

    this.suggesterManager = new PluginManager<>(
        config,
        Suggester.class,
        this::initializeSuggester
    );

    this.suggestersForProvider = new HashMap<>(providerManager.getPlugins().size() * 2);
  }

  private void initializeSuggester(Suggester suggester) throws InitializationException {
    Collection<String> dependencies = suggester.getDependencies();
    Map<String, Provider> loadedDependencies = new HashMap<>(dependencies.size() * 2);
    for (String dependencyName : dependencies) {
      Optional<Provider> foundDependency = this.providerManager.get(dependencyName);
      if (!foundDependency.isPresent()) {
        log.severe(String.format(
            "Missing dependency for suggester '%s': '%s'. This suggester will be deactivated.",
            suggester.getReadableName(),
            dependencyName
        ));
        return;
      }

      Provider dependency = foundDependency.get();
      suggestersForProvider.computeIfAbsent(dependency, s -> new LinkedList<>()).add(suggester);
      loadedDependencies.put(dependencyName, dependency);
    }

    for (String dependencyName : suggester.getOptionalDependencies()) {
      Optional<Provider> foundDependency = this.providerManager.get(dependencyName);
      if (foundDependency.isPresent()) {
        Provider dependency = foundDependency.get();
        suggestersForProvider.computeIfAbsent(dependency, s -> new LinkedList<>()).add(suggester);
        loadedDependencies.put(dependencyName, dependency);
      }
    }

    suggester.initialize(loadedDependencies);
  }

  @Nonnull
  public Provider getProvider(String name) {
    return this.providerManager.get(name)
        .orElseThrow(() -> new IllegalArgumentException("No such Provider: " + name));
  }

  @Nonnull
  public Map<String, Provider> getProviders() {
    return providerManager.getPlugins();
  }


  @Nonnull
  public Suggester getSuggester(String name) {
    return this.suggesterManager.get(name)
        .orElseThrow(() -> new IllegalArgumentException("No such Suggester: " + name));
  }

  @Nonnull
  public Map<String, Suggester> getSuggesters() {
    return suggesterManager.getPlugins();
  }

  @Nonnull
  public List<? extends Config.Entry> getConfigEntries(NamedPlugin plugin) {
    if (plugin instanceof Provider) {
      return providerManager.getConfigEntries((Provider) plugin);
    } else if (plugin instanceof Suggester) {
      return suggesterManager.getConfigEntries((Suggester) plugin);
    } else {
      throw new IllegalArgumentException();
    }
  }

  void initialize(NamedPlugin plugin) throws InitializationException {
    if (plugin instanceof Provider) {
      providerManager.initialize((Provider) plugin);
    } else if (plugin instanceof Suggester) {
      suggesterManager.initialize((Suggester) plugin);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void addStateListener(NamedPlugin plugin, BiConsumer<State, State> listener) {
    if (plugin instanceof Provider) {
      addStateListener((Provider) plugin, listener);
    } else if (plugin instanceof Suggester) {
      addStateListener((Suggester) plugin, listener);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void removeStateListener(NamedPlugin plugin, BiConsumer<State, State> listener) {
    if (plugin instanceof Provider) {
      removeStateListener((Provider) plugin, listener);
    } else if (plugin instanceof Suggester) {
      removeStateListener((Suggester) plugin, listener);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private boolean isActive(Provider provider) {
    return providerManager.isActive(provider);
  }

  private void addStateListener(Provider provider, BiConsumer<State, State> listener) {
    providerManager.addStateListener(provider, listener);
  }

  private void removeStateListener(Provider provider, BiConsumer<State, State> listener) {
    providerManager.removeStateListener(provider, listener);
  }

  private boolean isActive(Suggester provider) {
    return suggesterManager.isActive(provider);
  }

  private void addStateListener(Suggester suggester, BiConsumer<State, State> listener) {
    suggesterManager.addStateListener(suggester, listener);
  }

  private void removeStateListener(Suggester suggester, BiConsumer<State, State> listener) {
    suggesterManager.removeStateListener(suggester, listener);
  }

  @Nonnull
  public List<Suggester> getSuggestersFor(@Nonnull Provider provider) {
    return suggestersForProvider.getOrDefault(provider, Collections.emptyList());
  }

  @Override
  public void close() throws IOException {
    suggesterManager.close();
    providerManager.close();
  }

  @ParametersAreNonnullByDefault
  private static class PluginManager<T extends NamedPlugin> implements Closeable {

    @Nonnull
    private final Config config;
    @Nonnull
    private final Map<String, T> plugins;
    @Nonnull
    private final Map<T, State> states;
    @Nonnull
    private final Initializer<T> initializer;

    @Nonnull
    private final Map<T, Set<BiConsumer<State, State>>> listeners;

    private PluginManager(Config config, Class<T> type, Initializer<T> initializer) {
      this.config = config;
      String pluginFolderName = DefaultConfigEntry.get(config).pluginFolder.getOrDefault();
      File pluginFolder = new File(pluginFolderName);

      this.plugins = loadPlugins(pluginFolder, type);
      this.states = plugins.values().stream()
          .collect(Collectors.toMap(Function.identity(), p -> State.INACTIVE));
      this.initializer = initializer;

      this.listeners = new HashMap<>();
    }

    @Nonnull
    private Map<String, T> loadPlugins(File pluginFolder, Class<T> type) {
      PluginLoader<T> loader = new PluginLoader<>(pluginFolder, type);
      return loader.load().stream().collect(Collectors.toMap(
          NamedPlugin::getName,
          Function.identity()
      ));
    }

    @Nonnull
    public Optional<T> get(String name) {
      return Optional.ofNullable(plugins.get(name));
    }

    @Nonnull
    public Map<String, T> getPlugins() {
      return plugins;
    }

    @Nonnull
    private State getState(T t) {
      return states.get(t);
    }

    private void setState(T t, State state) {
      State old = getState(t);
      states.put(t, state);
      Set<BiConsumer<State, State>> listeners = this.listeners.get(t);
      if (listeners != null) {
        listeners.forEach(c -> c.accept(old, state));
      }
    }

    public void initialize(T t) throws InitializationException {
      State state = getState(t);
      switch (state) {
        case INACTIVE:
          initializeConfig(t);
        case CONFIG:
          initializer.initialize(t);
          setState(t, State.ACTIVE);
        case ACTIVE:
        default:
          break;
      }
    }

    public void close(T t) throws IOException {
      if (getState(t) == State.ACTIVE) {
        t.close();
        setState(t, State.CONFIG);
      }
    }

    @Nonnull
    public List<? extends Config.Entry> getConfigEntries(T t) {
      return initializeConfig(t);
    }

    public void destructConfigEntries(T t) {
      State state = getState(t);
      if (state == State.ACTIVE) {
        throw new IllegalStateException("Plugin is active");
      }

      if (state == State.CONFIG) {
        t.destructConfigEntries();
        setState(t, State.INACTIVE);
      }
    }

    @Nonnull
    private List<? extends Config.Entry> initializeConfig(T t) {
      List<? extends Config.Entry> entries = t.initializeConfigEntries(config);
      if (getState(t) == State.INACTIVE) {
        setState(t, State.CONFIG);
      }
      return entries;
    }

    public void addStateListener(T t, BiConsumer<State, State> listener) {
      Set<BiConsumer<State, State>> listeners = this.listeners
          .computeIfAbsent(t, p -> new HashSet<>());
      listeners.add(listener);
    }

    public void removeStateListener(T t, BiConsumer<State, State> listener) {
      Set<BiConsumer<State, State>> listeners = this.listeners.get(t);
      if (listeners != null) {
        listeners.remove(listener);
      }
    }

    public boolean isActive(T t) {
      return states.get(t) == State.ACTIVE;
    }

    @Override
    public void close() throws IOException {
      for (T plugin : plugins.values()) {
        try {
          close(plugin);
          destructConfigEntries(plugin);
        } catch (IOException e) {
          log.severe("Error closing plugin: " + e);
        }
      }
    }
  }

  @FunctionalInterface
  private interface Initializer<T extends NamedPlugin> {

    void initialize(T t) throws InitializationException;
  }
}
