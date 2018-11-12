package com.github.bjoernpetersen.jmusicbot

import com.github.bjoernpetersen.jmusicbot.config.Config
import com.github.bjoernpetersen.jmusicbot.platform.Platform
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactoryLoader
import mu.KotlinLogging
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.*

typealias PlaybackFactoryWrapperFactory = (PlaybackFactory) -> PlaybackFactoryWrapper

/**
 * Manages all PlaybackFactories for a MusicBot session.
 * @param config a config to use
 * @param wrapperFactory a factory supplying a [PlaybackFactoryWrapper] for a [PlaybackFactory]
 */
class PlaybackFactoryManager(config: Config, private val wrapperFactory: PlaybackFactoryWrapperFactory) :
    Closeable {

  private val factories: MutableSet<PlaybackFactoryWrapper> = LinkedHashSet()
  private val factoryByType: MutableMap<Class<out PlaybackFactory>, PlaybackFactoryWrapper> = LinkedHashMap()
  private val logger = KotlinLogging.logger {}

  /**
   * Returns an immutable Set of all PlaybackFactories.
   */
  val playbackFactories: Set<PlaybackFactoryWrapper>
    get() = Collections.unmodifiableSet(factories)

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
  fun hasFactory(factoryType: Class<out PlaybackFactory>): Boolean =
      factoryByType[factoryType]?.isActive == true

  /**
   * Completely closes all registered playback factories.
   */
  @Throws(IOException::class)
  override fun close() {
    for (playbackFactory in playbackFactories) {
      playbackFactory.close()
      playbackFactory.destructConfigEntries()
    }
  }

  private fun loadFactories(pluginFolder: File) {
    for (factory in PluginLoader(pluginFolder, PlaybackFactory::class.java).load()) {
      try {
        storeFactoryForValidBases(factory)
      } catch (e: InvalidFactoryException) {
        logger.warn(e) { "Could not load factory " + factory.readableName }
      } catch (e: InitializationException) {
        logger.warn(e) { "Could not load factory " + factory.readableName }
      }
    }

    val platform = Platform.get()
    for (loader in PluginLoader(pluginFolder, PlaybackFactoryLoader::class.java).load()) {
      val factory = loader.load(platform)
      if (factory == null) {
        logger.warn { "Platform not supported by PlaybackFactory ${loader.readableName}" }
      } else {
        try {
          storeFactoryForValidBases(factory)
        } catch (e: InvalidFactoryException) {
          logger.warn(e) { "Could not load factory " + factory.readableName }
        } catch (e: InitializationException) {
          logger.warn(e) { "Could not load factory " + factory.readableName }
        }
      }
    }
  }

  @Throws(InvalidFactoryException::class, InitializationException::class)
  private fun storeFactoryForValidBases(factory: PlaybackFactory) {
    val validBases = LinkedList<Class<out PlaybackFactory>>()
    for (base in factory.bases) {
      if (!base.isAssignableFrom(factory.javaClass)) {
        logger.warn { "Bad base '$base' for PlaybackFactory: $factory" }
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

  /**
   * Ensures all playback factories in the [Plugin.State.CONFIG] state are fully configured.
   * @param configurator a configurator to ask the user to fix missing config entries
   */
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
            logger.warn { "Deactivating unconfigured plugin " + factory.readableName }
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

  /**
   * Initializes all playback factories in the [Plugin.State.CONFIG] state.
   */
  @Throws(InterruptedException::class)
  fun initializeFactories(initStateWriter: InitStateWriter) {
    val defective = LinkedList<PlaybackFactory>()
    for (factory in playbackFactories.filter { it.state == Plugin.State.CONFIG }) {
      try {
        if (Thread.currentThread().isInterrupted) {
          throw InterruptedException()
        }
        initStateWriter.begin(factory)
        factory.initialize(initStateWriter)
      } catch (e: InitializationException) {
        logger.warn(e) { "Could not initialize PlaybackFactory '${factory.readableName}'" }
        defective.add(factory)
      } catch (e: InterruptedException) {
        initStateWriter.state("Interrupted during PlaybackFactory initialization, closing...")
        close()
        throw e
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
