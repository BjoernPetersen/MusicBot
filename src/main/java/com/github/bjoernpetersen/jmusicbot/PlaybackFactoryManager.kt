package com.github.bjoernpetersen.jmusicbot

import com.github.bjoernpetersen.jmusicbot.config.Config
import com.github.bjoernpetersen.jmusicbot.platform.Platform
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactoryLoader
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.*

typealias PlaybackFactoryWrapperFactory = (PlaybackFactory) -> PlaybackFactoryWrapper

class PlaybackFactoryManager(config: Config, private val wrapperFactory: PlaybackFactoryWrapperFactory) :
    Loggable, Closeable {

  private val factories: MutableSet<PlaybackFactoryWrapper> = LinkedHashSet()
  private val factoryByType: MutableMap<Class<out PlaybackFactory>, PlaybackFactoryWrapper> = LinkedHashMap()

  val playbackFactories: Set<PlaybackFactoryWrapper>
    get() = factories

  init {
    val pluginFolderName = config.defaults.pluginFolder.value
    val pluginFolder = File(pluginFolderName)
    loadFactories(pluginFolder)
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
    val result = factoryByType[factoryType]
    return if (result == null || !result.isActive) {
      throw IllegalArgumentException("No factory for: " + factoryType.simpleName)
    } else {
      try {
        factoryType.cast(result.wrapped)
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
  fun hasFactory(factoryType: Class<out PlaybackFactory>): Boolean = factoryByType[factoryType]?.isActive == true

  @Throws(IOException::class)
  override fun close() {
    for (playbackFactory in playbackFactories) {
      playbackFactory.close()
    }
    factories.clear()
    factoryByType.clear()
  }

  private fun loadFactories(pluginFolder: File) {
    for (factory in PluginLoader(pluginFolder, PlaybackFactory::class.java).load()) {
      try {
        storeFactoryForValidBases(factory)
      } catch (e: InvalidFactoryException) {
        logInfo(e, "Could not load factory " + factory.readableName)
      } catch (e: InitializationException) {
        logInfo(e, "Could not load factory " + factory.readableName)
      }
    }

    val platform = Platform.get()
    for (loader in PluginLoader(pluginFolder, PlaybackFactoryLoader::class.java).load()) {
      val factory = loader.load(platform)
      if (factory == null) {
        logInfo("Platform not supported by PlaybackFactory %s", loader.readableName)
      } else {
        try {
          storeFactoryForValidBases(factory)
        } catch (e: InvalidFactoryException) {
          logInfo(e, "Could not load factory %s", factory.readableName)
        } catch (e: InitializationException) {
          logInfo(e, "Could not load factory %s", factory.readableName)
        }
      }
    }
  }

  @Throws(InvalidFactoryException::class, InitializationException::class)
  private fun storeFactoryForValidBases(factory: PlaybackFactory) {
    val validBases = LinkedList<Class<out PlaybackFactory>>()
    for (base in factory.bases) {
      if (!base.isAssignableFrom(factory.javaClass)) {
        logWarning("Bad base '%s' for PlaybackFactory: %s", base, factory)
        continue
      }
      validBases.add(base)
    }

    if (validBases.isEmpty()) {
      throw InvalidFactoryException()
    }

    val wrapper = wrapperFactory(factory)
    factories.add(wrapper)
    for (base in validBases) {
      factoryByType.put(base, wrapper)
    }
  }

  @Throws(CancelException::class)
  fun ensureConfigured(configurator: Configurator) {
    val unconfigured = LinkedList<PlaybackFactory>()
    for (factory in playbackFactories.filter { it.state == Plugin.State.CONFIG }) {
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
          Configurator.Result.OK -> {
            // just continue
          }
        }
        missing = factory.missingConfigEntries
      }
    }

    for (factory in unconfigured) {
      removeFactory(factory)
    }
  }

  @Throws(InterruptedException::class)
  fun initializeFactories(initStateWriter: InitStateWriter) {
    val defective = LinkedList<PlaybackFactory>()
    for (factory in playbackFactories.filter { it.state == Plugin.State.CONFIG }) {
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
    }
  }

  private fun removeFactory(factory: PlaybackFactory) {
    for (base in factory.bases) {
      val registered = getFactory(base)
      if (registered === factory) {
        factoryByType.remove(base)
      }
    }
    factories.remove(factory)
    factory.destructConfigEntries()
  }

  private class InvalidFactoryException : Exception()
}
