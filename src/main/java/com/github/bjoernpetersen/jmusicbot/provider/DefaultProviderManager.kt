package com.github.bjoernpetersen.jmusicbot.provider

import com.github.bjoernpetersen.jmusicbot.*
import com.github.bjoernpetersen.jmusicbot.config.Config
import com.github.bjoernpetersen.jmusicbot.config.Config.Entry
import com.github.bjoernpetersen.jmusicbot.platform.Platform
import com.github.bjoernpetersen.jmusicbot.platform.Support
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory
import com.github.bjoernpetersen.jmusicbot.playback.SongEntry
import com.github.zafarkhaja.semver.Version
import java.io.File
import java.io.IOException
import java.util.*
import java.util.function.BiConsumer
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

internal class DefaultProviderManager(private val providerWrapperFactory: ProviderManager.ProviderWrapperFactory,
    private val suggesterWrapperFactory: ProviderManager.SuggesterWrapperFactory) : ProviderManager, Loggable {

  private val providerById: MutableMap<String, ProviderManager.ProviderWrapper> = HashMap(64)
  private val providerByBase: MutableMap<Class<out Provider>, Provider> = HashMap(64)
  private val suggesterById: MutableMap<String, ProviderManager.SuggesterWrapper> = HashMap(64)
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

  override fun getAllProviders(): Map<String, ProviderManager.ProviderWrapper> = providerById

  override fun getAllSuggesters(): Map<String, ProviderManager.SuggesterWrapper> = suggesterById

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

  override fun initializeProviders(initStateWriter: InitStateWriter) = providerById.values
      .filter { it.state == Plugin.State.CONFIG }
      .forEach {
        try {
          initStateWriter.begin(it.readableName)
          initStateWriter.state("Initializing provider ${it.readableName}...")
          it.initialize(initStateWriter, playbackFactoryManager)
        } catch (e: InitializationException) {
          logInfo(e, "Could not initialize Provider ${it.readableName}")
          removeProvider(it)
        } catch (e: RuntimeException) {
          logInfo(e, "Unexpected error initializing Provider ${it.readableName}")
          removeProvider(it)
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

  override fun initializeSuggesters(initStateWriter: InitStateWriter) {
    suggesterById.values
        .filter { it.state == Plugin.State.CONFIG }
        .forEach { s ->
          try {
            initStateWriter.begin(s.readableName)
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
          }
        }
  }

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

  override fun getProvider(id: String): ProviderManager.ProviderWrapper? = providerById[id]

  override fun getProvider(baseClass: Class<out Provider>): Provider? {
    val provider = providerByBase[baseClass] ?: return null
    val wrapper = getWrapper(provider)
    return if (wrapper.isActive) provider else null
  }

  override fun getSuggester(id: String): ProviderManager.SuggesterWrapper? = suggesterById[id]

  private fun close(plugin: PluginWrapper<*>) {
    try {
      if (plugin.isActive) {
        plugin.close()
      }
    } catch (e: IOException) {
      logSevere(e, "Error closing plugin %s", plugin.readableName)
    }
  }

  @Throws(IOException::class)
  override fun close() {
    for (provider in providerById.values) {
      close(provider)
    }

    for (suggester in suggesterById.values) {
      close(suggester)
    }
  }
}

open class DefaultPluginWrapper<T : Plugin> constructor(private val plugin: T) : PluginWrapper<T> {
  private val listeners: MutableSet<BiConsumer<Plugin.State, Plugin.State>> = HashSet()

  private var configEntries: List<Config.Entry> = emptyList()
  private var state: Plugin.State = Plugin.State.INACTIVE

  override fun getState(): Plugin.State = state

  protected fun setState(state: Plugin.State) {
    val old = this.state
    this.state = state
    listeners.forEach { l -> l.accept(old, state) }
  }

  override fun getWrapped(): T = plugin

  override fun getConfigEntries(): List<Config.Entry> = configEntries

  override fun initializeConfigEntries(config: Config): List<Entry> {
    if (getState() < Plugin.State.CONFIG) {
      configEntries = wrapped.initializeConfigEntries(config)
      setState(Plugin.State.CONFIG)
    }
    return configEntries
  }

  override fun getMissingConfigEntries(): List<Entry> = wrapped.missingConfigEntries

  override fun destructConfigEntries() {
    if (getState() > Plugin.State.CONFIG) {
      throw IllegalStateException()
    } else if (getState() < Plugin.State.CONFIG) {
      return
    }
    configEntries = emptyList()
    wrapped.destructConfigEntries()
    setState(Plugin.State.INACTIVE)
  }

  override fun addStateListener(listener: BiConsumer<Plugin.State, Plugin.State>) {
    listeners.add(listener)
  }

  override fun removeStateListener(listener: BiConsumer<Plugin.State, Plugin.State>) {
    listeners.remove(listener)
  }

  override fun getReadableName(): String = wrapped.readableName

  override fun getSupport(platform: Platform): Support = wrapped.getSupport(platform)

  override fun getMinSupportedVersion(): Version = wrapped.minSupportedVersion
  override fun getMaxSupportedVersion(): Version = wrapped.maxSupportedVersion

  @Throws(IOException::class)
  override fun close() {
    if (getState() < Plugin.State.ACTIVE) {
      return
    }
    try {
      wrapped.close()
    } finally {
      setState(Plugin.State.CONFIG)
    }
  }
}

open class DefaultProviderWrapper(plugin: Provider) : DefaultPluginWrapper<Provider>(plugin),
    ProviderManager.ProviderWrapper {

  private val _suggesters: MutableList<Suggester> = ArrayList(16)
  val suggesters: List<Suggester>
    get() = _suggesters

  fun addSuggester(suggester: Suggester) = _suggesters.add(suggester)

  override fun getPlaybackDependencies(): Set<Class<out PlaybackFactory>> = wrapped.playbackDependencies

  override fun getId(): String = wrapped.id

  @Throws(InitializationException::class, InterruptedException::class)
  override fun initialize(initStateWriter: InitStateWriter, manager: PlaybackFactoryManager) {
    if (state < Plugin.State.CONFIG) {
      throw IllegalStateException()
    } else if (state == Plugin.State.ACTIVE) {
      return
    }

    wrapped.initialize(initStateWriter, manager)
    state = Plugin.State.ACTIVE
  }

  override fun search(query: String): List<Song> = wrapped.search(query)

  @Throws(NoSuchSongException::class)
  override fun lookup(id: String): Song = wrapped.lookup(id)

  override fun getBaseClass(): Class<out Provider> = wrapped.baseClass
}

open class DefaultSuggesterWrapper(plugin: Suggester) : DefaultPluginWrapper<Suggester>(plugin),
    ProviderManager.SuggesterWrapper {

  override fun suggestNext(): Song = wrapped.suggestNext()

  override fun getNextSuggestions(maxLength: Int): List<Song> = wrapped.getNextSuggestions(maxLength)

  override fun getId(): String = wrapped.id

  @Throws(InitializationException::class, InterruptedException::class)
  override fun initialize(initStateWriter: InitStateWriter, dependencies: DependencyMap<Provider>) {
    if (state < Plugin.State.CONFIG) {
      throw IllegalStateException()
    } else if (state == Plugin.State.ACTIVE) {
      return
    }

    wrapped.initialize(initStateWriter, dependencies)
    state = Plugin.State.ACTIVE
  }

  override fun dislike(song: Song) {
    wrapped.dislike(song)
  }

  override fun notifyPlayed(entry: SongEntry) {
    wrapped.notifyPlayed(entry)
  }

  override fun removeSuggestion(song: Song) {
    wrapped.removeSuggestion(song)
  }

  override fun getDependencies(): Set<Class<out Provider>> = wrapped.dependencies

  override fun getOptionalDependencies(): Set<Class<out Provider>> = wrapped.optionalDependencies
}
