package com.github.bjoernpetersen.jmusicbot.provider

import com.github.bjoernpetersen.jmusicbot.*
import com.github.bjoernpetersen.jmusicbot.config.Config
import com.github.bjoernpetersen.jmusicbot.platform.Platform
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

internal class DefaultProviderManager(private val providerWrapperFactory: ProviderManager.ProviderWrapperFactory,
    private val suggesterWrapperFactory: ProviderManager.SuggesterWrapperFactory) : ProviderManager, Loggable {

  private val providerById: MutableMap<String, ProviderWrapper> = HashMap(64)
  private val providerByBase: MutableMap<Class<out Provider>, Provider> = HashMap(64)
  private val suggesterById: MutableMap<String, SuggesterWrapper> = HashMap(64)
  private val suggestersByProvider: MutableMap<Provider, MutableList<Suggester>> = HashMap(64)

  private lateinit var playbackFactoryManager: PlaybackFactoryManager

  override fun initialize(config: Config, manager: PlaybackFactoryManager) {
    this.playbackFactoryManager = manager

    val pluginFolderName = config.defaults.pluginFolder.value
    val pluginFolder = File(pluginFolderName)
    loadProviders(pluginFolder);
    loadSuggesters(pluginFolder);
  }

  private fun addProvider(provider: Provider) {
    val baseClass = provider.baseClass
    if (!baseClass.isInstance(provider)) {
      logInfo("Provider ${provider.readableName} does not implement its base class")
      return
    }
    providerByBase[baseClass] = provider
    providerById[provider.id] = providerWrapperFactory.make(provider)
  }

  private fun loadProviders(pluginFolder: File) {
    for (provider in PluginLoader(pluginFolder, Provider::class.java).load()) {
      addProvider(provider)
    }

    val platform = Platform.get()
    for (providerLoader in PluginLoader(pluginFolder, ProviderLoader::class.java).load()) {
      val provider = providerLoader.load(platform)
      if (provider != null) {
        addProvider(provider)
      } else {
        logInfo("ProviderLoader ${providerLoader.readableName} does not support platform.")
      }
    }
  }

  private fun loadSuggesters(pluginFolder: File) {
    for (suggester in PluginLoader(pluginFolder, Suggester::class.java).load()) {
      suggesterById[suggester.id] = suggesterWrapperFactory.make(suggester)
    }
  }

  override fun getAllProviders(): Map<String, ProviderWrapper> = providerById

  override fun getAllSuggesters(): Map<String, SuggesterWrapper> = suggesterById

  override fun getSuggesters(provider: Provider): Collection<Suggester> = suggestersByProvider[provider] ?: emptyList()

  private fun removeProvider(provider: Provider) {
    providerByBase.remove(provider.baseClass)
    providerById.remove(provider.id)
    provider.destructConfigEntries()
  }

  @Throws(CancelException::class)
  private fun <P : Plugin> ensureConfigured(plugin: P,
      configurator: Configurator,
      unconfigured: MutableList<P>) {
    var missing = plugin.missingConfigEntries
    while (!missing.isEmpty()) {
      missing = when (configurator.configure(plugin.readableName, missing)) {
        Configurator.Result.OK -> plugin.missingConfigEntries
        Configurator.Result.DISABLE -> {
          unconfigured.add(plugin)
          emptyList()
        }
        Configurator.Result.CANCEL -> throw CancelException("Config cancelled")
      }
    }
  }

  @Throws(CancelException::class)
  override fun ensureProvidersConfigured(configurator: Configurator) {
    val unconfigured = LinkedList<Provider>()
    allProviders.values
        .filter { it.state == Plugin.State.CONFIG }
        .forEach { ensureConfigured(it, configurator, unconfigured) }
    unconfigured.forEach(this::removeProvider)
  }

  @Throws(InterruptedException::class)
  override fun initializeProviders(initStateWriter: InitStateWriter) = providerById.values
      .filter { it.state == Plugin.State.CONFIG }
      .forEach {
        try {
          if (Thread.currentThread().isInterrupted) {
            throw InterruptedException()
          }
          initStateWriter.begin(it)
          initStateWriter.state("Initializing provider ${it.readableName}...")
          it.initialize(initStateWriter, playbackFactoryManager)
        } catch (e: InitializationException) {
          logInfo(e, "Could not initialize Provider ${it.readableName}")
          removeProvider(it)
        } catch (e: RuntimeException) {
          logInfo(e, "Unexpected error initializing Provider ${it.readableName}")
          removeProvider(it)
        } catch (e: InterruptedException) {
          initStateWriter.state("Interrupted during initialization. Closing...")
          close()
          throw e
        }
      }

  private fun removeSuggester(suggester: Suggester) {
    suggesterById.remove(suggester.id)
    suggester.destructConfigEntries()
  }

  @Throws(CancelException::class)
  override fun ensureSuggestersConfigured(configurator: Configurator) {
    val unconfigured = LinkedList<Suggester>()
    allSuggesters.values
        .filter { it.state == Plugin.State.CONFIG }
        .forEach { ensureConfigured(it, configurator, unconfigured) }
    unconfigured.forEach(this::removeSuggester)
  }

  @Throws(InterruptedException::class)
  override fun initializeSuggesters(initStateWriter: InitStateWriter) {
    suggesterById.values
        .filter { it.state == Plugin.State.CONFIG }
        .forEach { s ->
          try {
            if (Thread.currentThread().isInterrupted) {
              throw InterruptedException()
            }
            initStateWriter.begin(s)
            initStateWriter.state("Initializing ${s.readableName}...")
            val dependencies = buildDependencies(s)
            s.initialize(initStateWriter, DependencyMap(dependencies))
            dependencies.values.forEach {
              suggestersByProvider.computeIfAbsent(it, { LinkedList() }).add(s)
            }
          } catch (e: InitializationException) {
            logInfo(e, "Could not initialize Suggester ${s.readableName}")
            removeSuggester(s)
          } catch (e: RuntimeException) {
            logInfo(e, "Unexpected error initializing Suggester ${s.readableName}")
            removeSuggester(s)
          } catch (e: InterruptedException) {
            initStateWriter.state("Interrupted during initialization. Closing...")
            close()
            throw e
          }
        }
  }

  @Throws(InitializationException::class)
  private fun buildDependencies(suggester: Suggester): Map<Class<out Provider>, Provider> {
    val dependencies = suggester.dependencies
    val loadedDependencies = LinkedHashMap<Class<out Provider>, Provider>(dependencies.size * 2)
    for (dependencyClass in dependencies) {
      val dependency = getProvider(dependencyClass) ?: throw InitializationException(
          "Missing dependency for suggester ${suggester.readableName}: ${dependencyClass.name}."
      )

      loadedDependencies.put(dependencyClass, dependency)
    }

    for (dependencyClass in suggester.optionalDependencies) {
      val dependency = getProvider(dependencyClass)
      if (dependency != null) {
        loadedDependencies.put(dependencyClass, dependency)
      }
    }

    return loadedDependencies
  }

  override fun getProvider(id: String): ProviderWrapper? = providerById[id]

  override fun getProvider(baseClass: Class<out Provider>): Provider? {
    val provider = providerByBase[baseClass] ?: return null
    val wrapper = getWrapper(provider)
    return if (wrapper.isActive) provider else null
  }

  override fun getSuggester(id: String): SuggesterWrapper? = suggesterById[id]

  /**
   * Completely close a plugin (close() and destructConfigEntries()).
   */
  private fun close(plugin: PluginWrapper<*>) {
    try {
      if (plugin.state == Plugin.State.ACTIVE) {
        plugin.close()
      }
      if (plugin.state == Plugin.State.CONFIG) {
        plugin.destructConfigEntries()
      }
    } catch (e: IOException) {
      logSevere(e, "Error closing plugin %s", plugin.readableName)
    }
  }

  @Throws(IOException::class)
  override fun close() {
    for (provider in allProviders.values) {
      close(provider)
    }

    for (suggester in allSuggesters.values) {
      close(suggester)
    }
  }
}


