package com.github.bjoernpetersen.jmusicbot

import com.github.bjoernpetersen.jmusicbot.config.Config
import com.github.bjoernpetersen.jmusicbot.platform.Platform
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactoryLoader
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.*

class PlaybackFactoryManager(private val config: Config, included: Collection<PlaybackFactory>) : Loggable, Closeable {

  private val factories: MutableMap<Class<out PlaybackFactory>, PlaybackFactory> = HashMap()
  private val configEntries: Map<PlaybackFactory, List<Config.Entry>>

  val playbackFactories: Collection<PlaybackFactory>
    get() = Collections.unmodifiableCollection(configEntries.keys)

  init {
    val pluginFolderName = config.defaults.pluginFolder.value
    val pluginFolder = File(pluginFolderName)
    this.configEntries = loadFactories(pluginFolder, included)
  }

  /**
   * Gets a PlaybackFactory that implements the given marker interface.
   *
   * @param factoryType a PlaybackFactory marker interface
   * @param <F> the type of PlaybackFactory
   * @return an instance of F
   * @throws IllegalArgumentException if no implementation of the given interface is active.
   */
  fun <F : PlaybackFactory> getFactory(factoryType: Class<F>): F {
    val result = factories[factoryType]
    return if (result == null) {
      throw IllegalArgumentException("No factory for: " + factoryType.simpleName)
    } else {
      try {
        factoryType.cast(result)
      } catch (e: ClassCastException) {
        throw IllegalArgumentException("Wrong type for factory: " + factoryType.name, e)
      }
    }
  }

  /**
   * Checks whether there is an active factory implementing the specified type.
   *
   * @param factoryType a PlaybackFactory marker interface
   * @return whether an implementing factory is active
   */
  fun hasFactory(factoryType: Class<out PlaybackFactory>): Boolean = factories.containsKey(factoryType)

  fun getConfigEntries(factory: PlaybackFactory): List<Config.Entry> =
      configEntries[factory] ?: throw IllegalArgumentException("Unknown factory: ${factory.readableName}")

  @Throws(IOException::class)
  override fun close() {
    for (playbackFactory in playbackFactories) {
      playbackFactory.close()
    }
    factories.clear()
  }

  private fun loadFactories(pluginFolder: File,
      includedFactories: Collection<PlaybackFactory>): Map<PlaybackFactory, List<Config.Entry>> {
    val result = HashMap<PlaybackFactory, List<Config.Entry>>()
    for (factory in includedFactories) {
      try {
        result.put(factory, storeFactoryForValidBases(factory))
      } catch (e: InvalidFactoryException) {
        logSevere(e, "Could not load included factory " + factory.readableName)
      } catch (e: InitializationException) {
        logSevere(e, "Could not load included factory " + factory.readableName)
      }
    }

    val factories = PluginLoader(pluginFolder, PlaybackFactory::class.java).load()

    for (factory in factories) {
      try {
        result.put(factory, storeFactoryForValidBases(factory))
      } catch (e: InvalidFactoryException) {
        logInfo(e, "Could not load factory " + factory.readableName)
      } catch (e: InitializationException) {
        logInfo(e, "Could not load factory " + factory.readableName)
      }

    }

    val platform = Platform.get()
    for (loader in PluginLoader(pluginFolder,
        PlaybackFactoryLoader::class.java).load()) {
      val factory = loader.load(platform)
      if (factory == null) {
        logInfo("Platform not supported by PlaybackFactory %s", loader.readableName)
      } else {
        try {
          result.put(factory, storeFactoryForValidBases(factory))
        } catch (e: InvalidFactoryException) {
          logInfo(e, "Could not load factory %s", factory.readableName)
        } catch (e: InitializationException) {
          logInfo(e, "Could not load factory %s", factory.readableName)
        }
      }
    }

    return result
  }

  @Throws(PlaybackFactoryManager.InvalidFactoryException::class, InitializationException::class)
  private fun storeFactoryForValidBases(factory: PlaybackFactory): List<Config.Entry> {
    val validBases = LinkedList<Class<out PlaybackFactory>>()
    for (base in factory.bases) {
      if (!base.isAssignableFrom(factory.javaClass)) {
        logSevere("Bad base '%s' for PlaybackFactory: %s", base, factory)
        continue
      }
      validBases.add(base)
    }

    if (validBases.isEmpty()) {
      throw InvalidFactoryException()
    }

    val result = factory.initializeConfigEntries(config)

    for (base in validBases) {
      factories.put(base, factory)
    }

    return result
  }

  @Throws(CancelException::class)
  fun ensureConfigured(configurator: Configurator) {
    val unconfigured = LinkedList<PlaybackFactory>()
    for (factory in factories.values) {
      var missing: List<Config.Entry> = factory.missingConfigEntries
      wh@ while (!missing.isEmpty()) {
        val result = configurator.configure(factory.readableName, missing)
        when (result) {
          Configurator.Result.CANCEL -> throw CancelException("User cancelled configuration")
          Configurator.Result.DISABLE -> {
            logInfo("Deactivating unconfigured plugin " + factory.readableName)
            unconfigured.add(factory)
            break@wh // I feel dirty
          }
          else -> {
            // just continue
          }
        }
        missing = factory.missingConfigEntries
      }
    }

    for (factory in unconfigured) {
      removeFactory(factory)
      factory.destructConfigEntries()
    }
  }

  @Throws(InterruptedException::class)
  fun initializeFactories(initStateWriter: InitStateWriter) {
    val defective = LinkedList<PlaybackFactory>()
    for (factory in factories.values) {
      try {
        initStateWriter.begin(factory.readableName)
        factory.initialize(initStateWriter)
      } catch (e: InitializationException) {
        logWarning(e, "Could not initialize PlaybackFactory '%s'", factory)
        defective.add(factory)
      }
    }

    for (factory in defective) {
      removeFactory(factory)
      factory.destructConfigEntries()
    }
  }

  private fun removeFactory(factory: PlaybackFactory) {
    for (base in factory.bases) {
      val registered = getFactory(base)
      if (registered === factory) {
        factories.remove(base)
      }
    }
  }

  private class InvalidFactoryException : Exception()
}
