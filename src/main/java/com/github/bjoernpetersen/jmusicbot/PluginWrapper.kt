package com.github.bjoernpetersen.jmusicbot

import com.github.bjoernpetersen.jmusicbot.config.Config
import com.github.bjoernpetersen.jmusicbot.platform.Platform
import com.github.bjoernpetersen.jmusicbot.platform.Support
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory
import com.github.bjoernpetersen.jmusicbot.playback.SongEntry
import com.github.bjoernpetersen.jmusicbot.provider.DependencyMap
import com.github.bjoernpetersen.jmusicbot.provider.NoSuchSongException
import com.github.bjoernpetersen.jmusicbot.provider.Provider
import com.github.bjoernpetersen.jmusicbot.provider.Suggester
import com.github.zafarkhaja.semver.Version
import java.io.IOException
import java.util.HashSet
import kotlin.collections.ArrayList

typealias StateListener = (Plugin.State, Plugin.State) -> Unit
/**
 * Wrapper for a plugin which provides access to the plugin's state and delegates all calls to the
 * wrapped instance.
 *
 * @param <T> the type of the wrapped plugin
</T> */
interface PluginWrapper<T : Plugin> : Plugin {

  val state: Plugin.State
  /**
   * Convenience method to check whether the current state is [Plugin.State.ACTIVE].
   *
   * @return whether the wrapped plugin is active
   */
  val isActive: Boolean
    get() = state == Plugin.State.ACTIVE
  val wrapped: T
  val configEntries: List<Config.Entry>
  fun addStateListener(listener: StateListener)
  fun removeStateListener(listener: StateListener)
}

sealed class DefaultPluginWrapper<T : Plugin> constructor(override val wrapped: T) : PluginWrapper<T> {
  private val listeners: MutableSet<StateListener> = HashSet()

  override var configEntries: List<Config.Entry> = emptyList()
  override var state: Plugin.State = Plugin.State.INACTIVE
    protected set(value) {
      val old = this.state
      field = state
      listeners.forEach { it(old, state) }
    }

  override fun initializeConfigEntries(config: Config): List<Config.Entry> {
    if (state < Plugin.State.CONFIG) {
      configEntries = wrapped.initializeConfigEntries(config)
      state = Plugin.State.CONFIG
    }
    return configEntries
  }

  override fun getMissingConfigEntries(): List<Config.Entry> = wrapped.missingConfigEntries

  override fun destructConfigEntries() {
    if (state > Plugin.State.CONFIG) {
      throw IllegalStateException()
    } else if (state < Plugin.State.CONFIG) {
      return
    }
    configEntries = emptyList()
    wrapped.destructConfigEntries()
    state = Plugin.State.INACTIVE
  }

  override fun addStateListener(listener: StateListener) {
    listeners.add(listener)
  }

  override fun removeStateListener(listener: StateListener) {
    listeners.remove(listener)
  }

  override fun getReadableName(): String = wrapped.readableName

  override fun getSupport(platform: Platform): Support = wrapped.getSupport(platform)

  override fun getMinSupportedVersion(): Version = wrapped.minSupportedVersion
  override fun getMaxSupportedVersion(): Version = wrapped.maxSupportedVersion

  @Throws(IOException::class)
  override fun close() {
    if (state < Plugin.State.ACTIVE) {
      return
    }
    try {
      wrapped.close()
    } finally {
      state = Plugin.State.CONFIG
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DefaultPluginWrapper<*>

    if (wrapped != other.wrapped) return false

    return true
  }

  override fun hashCode(): Int = wrapped.hashCode()
}

interface PlaybackFactoryWrapper : PluginWrapper<PlaybackFactory>, PlaybackFactory
open class DefaultPlaybackFactoryWrapper(plugin: PlaybackFactory) : DefaultPluginWrapper<PlaybackFactory>(plugin),
    PlaybackFactoryWrapper {

  @Throws(InitializationException::class, InterruptedException::class)
  override fun initialize(initStateWriter: InitStateWriter) {
    if (state < Plugin.State.CONFIG) {
      throw IllegalStateException()
    } else if (state == Plugin.State.ACTIVE) {
      return
    }

    wrapped.initialize(initStateWriter)
    state = Plugin.State.ACTIVE
  }

  override fun getBases(): Collection<Class<out PlaybackFactory>> = wrapped.bases
}

interface ProviderWrapper : PluginWrapper<Provider>, Provider
open class DefaultProviderWrapper(plugin: Provider) : DefaultPluginWrapper<Provider>(plugin), ProviderWrapper {

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

interface SuggesterWrapper : PluginWrapper<Suggester>, Suggester
open class DefaultSuggesterWrapper(plugin: Suggester) : DefaultPluginWrapper<Suggester>(plugin), SuggesterWrapper {

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

interface AdminPluginWrapper : PluginWrapper<AdminPlugin>, AdminPlugin
open class DefaultAdminPluginWrapper(plugin: AdminPlugin) : DefaultPluginWrapper<AdminPlugin>(plugin),
    AdminPluginWrapper {

  @Throws(InitializationException::class, InterruptedException::class)
  override fun initialize(writer: InitStateWriter, musicBot: MusicBot) {
    if (state < Plugin.State.CONFIG) {
      throw IllegalStateException()
    } else if (state == Plugin.State.ACTIVE) {
      return
    }

    wrapped.initialize(writer, musicBot)
    state = Plugin.State.ACTIVE
  }
}
