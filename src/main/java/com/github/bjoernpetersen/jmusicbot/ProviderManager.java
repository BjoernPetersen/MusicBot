package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import com.github.bjoernpetersen.jmusicbot.provider.Suggester;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public final class ProviderManager implements Closeable {

  @Nonnull
  private static final Logger log = Logger.getLogger(ProviderManager.class.getName());

  @Nonnull
  private final Config config;
  @Nonnull
  private final PlaybackFactoryManager playbackFactoryManager;

  @Nonnull
  private final Map<String, Provider> providers;
  @Nonnull
  private final Map<String, Suggester> suggesters;

  @Nonnull
  private final Map<Provider, List<Suggester>> suggestersForProvider;

  ProviderManager(@Nonnull Config config,
      @Nonnull PlaybackFactoryManager playbackFactoryManager,
      @Nonnull List<Provider> providers,
      @Nonnull List<Suggester> suggesters) {
    this.config = config;
    this.playbackFactoryManager = playbackFactoryManager;

    this.providers = initializeProviders(providers);
    this.suggestersForProvider = new HashMap<>(providers.size() * 2);

    this.suggesters = initializeSuggesters(suggesters);
  }

  @Nonnull
  private Map<String, Provider> initializeProviders(List<Provider> providers) {
    Map<String, Provider> result = new LinkedHashMap<>(providers.size() * 2);
    for (Provider provider : providers) {
      try {
        provider.initialize(playbackFactoryManager);
        result.put(provider.getName(), provider);
      } catch (InitializationException e) {
        log.severe(String.format(
            "Error while initializing Provider '%s': %s", provider.getReadableName(), e
        ));
      }
    }
    return Collections.unmodifiableMap(result);
  }

  @Nonnull
  private Map<String, Suggester> initializeSuggesters(List<Suggester> suggesters) {
    Map<String, Suggester> result = new LinkedHashMap<>(suggesters.size() * 2);
    for (Suggester suggester : suggesters) {
      try {
        initializeSuggester(suggester);
        result.put(suggester.getName(), suggester);
      } catch (InitializationException e) {
        log.severe(String.format(
            "Error while initializing Suggester '%s': %s", suggester.getReadableName(), e
        ));
      }
    }
    return Collections.unmodifiableMap(result);
  }

  private void initializeSuggester(Suggester suggester) throws InitializationException {
    Collection<String> dependencies = suggester.getDependencies();
    Map<String, Provider> loadedDependencies = new HashMap<>(dependencies.size() * 2);
    for (String dependencyName : dependencies) {
      Provider dependency = this.providers.get(dependencyName);
      if (dependency == null) {
        log.severe(String.format(
            "Missing dependency for suggester '%s': '%s'. This suggester will be deactivated.",
            suggester.getReadableName(),
            dependencyName
        ));
        return;
      }

      suggestersForProvider.computeIfAbsent(dependency, s -> new LinkedList<>()).add(suggester);
      loadedDependencies.put(dependencyName, dependency);
    }

    for (String dependencyName : suggester.getOptionalDependencies()) {
      Provider dependency = this.providers.get(dependencyName);
      if (dependency != null) {
        suggestersForProvider.computeIfAbsent(dependency, s -> new LinkedList<>()).add(suggester);
        loadedDependencies.put(dependencyName, dependency);
      }
    }

    suggester.initialize(loadedDependencies);
  }

  @Nonnull
  public Provider getProvider(String name) {
    Provider provider = this.providers.get(name);
    if (provider == null) {
      throw new IllegalArgumentException("No such Provider: " + name);
    }
    return provider;
  }

  @Nonnull
  public Map<String, Provider> getProviders() {
    return providers;
  }

  @Nonnull
  public Suggester getSuggester(String name) {
    Suggester suggester = this.suggesters.get(name);
    if (suggester == null) {
      throw new IllegalArgumentException("No such Suggester: " + name);
    }
    return suggester;
  }

  @Nonnull
  public Map<String, Suggester> getSuggesters() {
    return suggesters;
  }

  @Nonnull
  public List<Suggester> getSuggestersFor(@Nonnull Provider provider) {
    return suggestersForProvider.getOrDefault(provider, Collections.emptyList());
  }

  @Override
  public void close() throws IOException {
    for (Suggester suggester : suggesters.values()) {
      try {
        suggester.close();
        suggester.destructConfigEntries();
      } catch (IOException e) {
        log.severe("Error closing suggester " + suggester.getName() + ": " + e);
      }
    }
    for (Provider provider : providers.values()) {
      try {
        provider.close();
        provider.destructConfigEntries();
      } catch (IOException e) {
        log.severe("Error closing provider " + provider.getName() + ": " + e);
      }
    }
  }
}
