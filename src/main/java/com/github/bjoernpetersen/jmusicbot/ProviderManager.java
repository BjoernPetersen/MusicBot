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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

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
        (sw, p) -> p.initialize(sw, playbackFactoryManager)
    );

    this.suggesterManager = new PluginManager<>(
        config,
        Suggester.class,
        this::initializeSuggester
    );

    this.suggestersForProvider = new HashMap<>(providerManager.getPlugins().size() * 2);
  }

  private void initializeSuggester(@Nonnull InitStateWriter initStateWriter,
      @Nonnull Suggester suggester) throws InitializationException, InterruptedException {
    Collection<String> dependencies = suggester.getDependencies();
    Map<String, Provider> loadedDependencies = new HashMap<>(dependencies.size() * 2);
    for (String dependencyName : dependencies) {
      Optional<Provider> foundDependency = this.providerManager.get(dependencyName);
      if (!foundDependency.isPresent()) {
        throw new InitializationException(String.format(
            "Missing dependency for suggester '%s': '%s'.",
            suggester.getReadableName(),
            dependencyName
        ));
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

    suggester.initialize(initStateWriter, loadedDependencies);
  }

  /**
   * Gets the provider with the specified name.
   * Only returns active providers.
   *
   * @param name the provider name
   * @return a Provider
   * @throws IllegalArgumentException if there is no such provider
   */
  @Nonnull
  public Provider getProvider(@Nonnull String name) {
    return this.providerManager.get(name)
        .orElseThrow(() -> new IllegalArgumentException("No such Provider: " + name));
  }

  @Nonnull
  public Map<String, Provider> getActiveProviders() {
    return this.providerManager.getActivePlugins();
  }

  @Nonnull
  public Map<String, Provider> getProviders() {
    return providerManager.getPlugins();
  }

  /**
   * Gets the suggester with the specified name.
   * Only returns active suggesters.
   *
   * @param name the suggester name
   * @return a Suggester
   * @throws IllegalArgumentException if there is no such suggester
   */
  @Nonnull
  public Suggester getSuggester(@Nonnull String name) {
    return this.suggesterManager.get(name)
        .orElseThrow(() -> new IllegalArgumentException("No such Suggester: " + name));
  }

  @Nonnull
  public Map<String, Suggester> getActiveSuggesters() {
    return this.suggesterManager.getActivePlugins();
  }

  @Nonnull
  public Map<String, Suggester> getSuggesters() {
    return suggesterManager.getPlugins();
  }

  @Nonnull
  public List<? extends Config.Entry> getConfigEntries(@Nonnull NamedPlugin plugin) {
    if (plugin instanceof Provider) {
      return providerManager.getConfigEntries((Provider) plugin);
    } else if (plugin instanceof Suggester) {
      return suggesterManager.getConfigEntries((Suggester) plugin);
    } else {
      throw new IllegalArgumentException();
    }
  }

  void initialize(@Nonnull NamedPlugin plugin, @Nonnull InitStateWriter initStateWriter)
      throws InitializationException, InterruptedException {
    initStateWriter.begin(plugin.getReadableName());
    if (plugin instanceof Provider) {
      providerManager.initialize(initStateWriter, (Provider) plugin);
    } else if (plugin instanceof Suggester) {
      suggesterManager.initialize(initStateWriter, (Suggester) plugin);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void destructConfigEntries(@Nonnull NamedPlugin plugin) {
    if (plugin instanceof Provider) {
      providerManager.destructConfigEntries((Provider) plugin);
    } else if (plugin instanceof Suggester) {
      suggesterManager.destructConfigEntries((Suggester) plugin);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void addStateListener(@Nonnull NamedPlugin plugin,
      @Nonnull BiConsumer<State, State> listener) {
    if (plugin instanceof Provider) {
      addStateListener((Provider) plugin, listener);
    } else if (plugin instanceof Suggester) {
      addStateListener((Suggester) plugin, listener);
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void removeStateListener(@Nonnull NamedPlugin plugin,
      @Nonnull BiConsumer<State, State> listener) {
    if (plugin instanceof Provider) {
      removeStateListener((Provider) plugin, listener);
    } else if (plugin instanceof Suggester) {
      removeStateListener((Suggester) plugin, listener);
    } else {
      throw new IllegalArgumentException();
    }
  }

  @Nonnull
  public State getState(@Nonnull NamedPlugin plugin) {
    if (plugin instanceof Provider) {
      return providerManager.getState((Provider) plugin);
    } else if (plugin instanceof Suggester) {
      return suggesterManager.getState((Suggester) plugin);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private void addStateListener(@Nonnull Provider provider,
      @Nonnull BiConsumer<State, State> listener) {
    providerManager.addStateListener(provider, listener);
  }

  private void removeStateListener(@Nonnull Provider provider,
      @Nonnull BiConsumer<State, State> listener) {
    providerManager.removeStateListener(provider, listener);
  }

  private void addStateListener(@Nonnull Suggester suggester,
      @Nonnull BiConsumer<State, State> listener) {
    suggesterManager.addStateListener(suggester, listener);
  }

  private void removeStateListener(@Nonnull Suggester suggester,
      @Nonnull BiConsumer<State, State> listener) {
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

    private PluginManager(@Nonnull Config config, @Nonnull Class<T> type,
        @Nonnull Initializer<T> initializer) {
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
    private Map<String, T> loadPlugins(@Nonnull File pluginFolder, @Nonnull Class<T> type) {
      PluginLoader<T> loader = new PluginLoader<>(pluginFolder, type);
      return loader.load().stream().collect(Collectors.toMap(
          NamedPlugin::getName,
          Function.identity()
      ));
    }

    @Nonnull
    public Optional<T> get(@Nonnull String name) {
      Optional<T> result = Optional.ofNullable(plugins.get(name));
      if (result.isPresent() && getState(result.get()) != State.ACTIVE) {
        return Optional.empty();
      } else {
        return result;
      }
    }

    @Nonnull
    public Map<String, T> getPlugins() {
      return plugins;
    }

    @Nonnull
    public Map<String, T> getActivePlugins() {
      return states.entrySet().stream()
          .filter(e -> e.getValue() == State.ACTIVE)
          .collect(Collectors.toMap(
              e -> e.getKey().getName(),
              Entry::getKey
          ));
    }

    @Nonnull
    private State getState(@Nonnull T t) {
      return states.get(t);
    }

    private void setState(@Nonnull T t, @Nonnull State state) {
      State old = getState(t);
      states.put(t, state);
      Set<BiConsumer<State, State>> listeners = this.listeners.get(t);
      if (listeners != null) {
        listeners.forEach(c -> c.accept(old, state));
      }
    }

    public void initialize(@Nonnull InitStateWriter initStateWriter, @Nonnull T t)
        throws InitializationException, InterruptedException {
      State state = getState(t);
      switch (state) {
        case INACTIVE:
          initializeConfig(t);
        case CONFIG:
          initializer.initialize(initStateWriter, t);
          setState(t, State.ACTIVE);
        case ACTIVE:
        default:
          break;
      }
    }

    public void close(@Nonnull T t) throws IOException {
      if (getState(t) == State.ACTIVE) {
        t.close();
        setState(t, State.CONFIG);
      }
    }

    @Nonnull
    public List<? extends Config.Entry> getConfigEntries(@Nonnull T t) {
      return initializeConfig(t);
    }

    public void destructConfigEntries(@Nonnull T t) {
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
    private List<? extends Config.Entry> initializeConfig(@Nonnull T t) {
      List<? extends Config.Entry> entries = t.initializeConfigEntries(config);
      if (getState(t) == State.INACTIVE) {
        setState(t, State.CONFIG);
      }
      return entries;
    }

    public void addStateListener(@Nonnull T t, @Nonnull BiConsumer<State, State> listener) {
      Set<BiConsumer<State, State>> listeners = this.listeners
          .computeIfAbsent(t, p -> new HashSet<>());
      listeners.add(listener);
    }

    public void removeStateListener(@Nonnull T t, @Nonnull BiConsumer<State, State> listener) {
      Set<BiConsumer<State, State>> listeners = this.listeners.get(t);
      if (listeners != null) {
        listeners.remove(listener);
      }
    }

    public boolean isActive(@Nonnull T t) {
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

    void initialize(@Nonnull InitStateWriter initStateWriter, @Nonnull T t)
        throws InitializationException, InterruptedException;
  }
}
