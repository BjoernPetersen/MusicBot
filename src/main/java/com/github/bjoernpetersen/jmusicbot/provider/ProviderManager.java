package com.github.bjoernpetersen.jmusicbot.provider;

import com.github.bjoernpetersen.jmusicbot.Configurator;
import com.github.bjoernpetersen.jmusicbot.InitStateWriter;
import com.github.bjoernpetersen.jmusicbot.PlaybackFactoryManager;
import com.github.bjoernpetersen.jmusicbot.Plugin.State;
import com.github.bjoernpetersen.jmusicbot.PluginLoader;
import com.github.bjoernpetersen.jmusicbot.PluginWrapper;
import com.github.bjoernpetersen.jmusicbot.ProviderWrapper;
import com.github.bjoernpetersen.jmusicbot.SuggesterWrapper;
import com.github.bjoernpetersen.jmusicbot.config.Config;
import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manages all instances of Providers and Suggesters and provides {@link PluginWrapper} instances for them.
 */
public interface ProviderManager extends Closeable {

  /**
   * Initialize the ProviderManager. This includes loading Providers and Suggesters with a {@link PluginLoader}
   * instance.
   *
   * @param config a Config instance
   * @param manager a PlaybackFactoryManager to initialize Providers later
   */
  void initialize(@Nonnull Config config, @Nonnull PlaybackFactoryManager manager);

  /**
   * Gets a map from provider ID to provider with all providers, regardless of state.
   *
   * @return a map (providerId -> provider)
   */
  @Nonnull
  Map<String, ? extends ProviderWrapper> getAllProviders();

  /**
   * Gets a map from suggester ID to suggester with all suggesters, regardless of state.
   *
   * @return a map (suggesterId -> suggester)
   */
  @Nonnull
  Map<String, ? extends SuggesterWrapper> getAllSuggesters();

  /**
   * Gets all active suggesters for the specified provider.
   *
   * @param provider a Provider
   * @return a collection of suggesters
   */
  @Nonnull
  Collection<? extends Suggester> getSuggesters(@Nonnull Provider provider);

  /**
   * Ensures all providers in the {@link State#CONFIG} state are fully configured.
   *
   * @param configurator a configurator to ask the user to fix missing config entries
   */
  void ensureProvidersConfigured(@Nonnull Configurator configurator);

  /**
   * Initializes all providers that are currently in the {@link State#CONFIG} state.
   *
   * @param initStateWriter an InitStateWriter
   */
  void initializeProviders(@Nonnull InitStateWriter initStateWriter) throws InterruptedException;

  /**
   * Ensures all suggesters in the {@link State#CONFIG} state are fully configured.
   *
   * @param configurator a configurator to ask the user to fix missing config entries
   */
  void ensureSuggestersConfigured(@Nonnull Configurator configurator);

  /**
   * Initializes all suggesters that are currently in the {@link State#CONFIG} state.
   *
   * @param initStateWriter an InitStateWriter
   */
  void initializeSuggesters(@Nonnull InitStateWriter initStateWriter) throws InterruptedException;

  /**
   * Gets all active providers.
   *
   * @return a stream of providers
   */
  @Nonnull
  default Stream<? extends Provider> getProviders() {
    return getAllProviders().values().stream()
        .filter(PluginWrapper::isActive);
  }

  /**
   * Gets all active suggesters.
   *
   * @return a stream of suggesters
   */
  @Nonnull
  default Stream<? extends Suggester> getSuggesters() {
    return getAllSuggesters().values().stream()
        .filter(PluginWrapper::isActive);
  }

  /**
   * Gets the provider with the specified name. Only returns active providers.
   *
   * @param id the provider ID
   * @return a Provider
   * @throws IllegalArgumentException if there is no such provider
   */
  @Nullable
  ProviderWrapper getProvider(@Nonnull String id);

  /**
   * Gets the provider implementing the specified base class. Only returns active providers. This method is guaranteed
   * not to return a {@link PluginWrapper} instance.
   *
   * @param baseClass a provider base class
   * @return a Provider, or null
   */
  @Nullable
  Provider getProvider(@Nonnull Class<? extends Provider> baseClass);

  /**
   * Gets the suggester with the specified name. Only returns active suggesters.
   *
   * @param id the suggester ID
   * @return a Suggester
   * @throws IllegalArgumentException if there is no such suggester
   */
  @Nullable
  SuggesterWrapper getSuggester(@Nonnull String id);

  @Nonnull
  default ProviderWrapper getWrapper(@Nonnull Provider provider) {
    if (provider instanceof ProviderWrapper) {
      return (ProviderWrapper) provider;
    } else {
      ProviderWrapper result = getProvider(provider.getId());
      if (result == null) {
        throw new IllegalArgumentException("Provider not found: " + provider.getId());
      }
      return result;
    }
  }

  @Nonnull
  default SuggesterWrapper getWrapper(@Nonnull Suggester suggester) {
    if (suggester instanceof SuggesterWrapper) {
      return (SuggesterWrapper) suggester;
    } else {
      SuggesterWrapper result = getSuggester(suggester.getId());
      if (result == null) {
        throw new IllegalArgumentException("Provider not found: " + suggester.getId());
      }
      return result;
    }
  }

  @FunctionalInterface
  interface ProviderWrapperFactory {

    @Nonnull
    ProviderWrapper make(@Nonnull Provider provider);
  }

  @FunctionalInterface
  interface SuggesterWrapperFactory {

    @Nonnull
    SuggesterWrapper make(@Nonnull Suggester suggester);
  }

  @Nonnull
  static ProviderManager defaultManager(ProviderWrapperFactory providerWrapperFactory,
      SuggesterWrapperFactory suggesterWrapperFactory) {
    return new DefaultProviderManager(providerWrapperFactory, suggesterWrapperFactory);
  }
}
