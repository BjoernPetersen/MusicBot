package net.bjoernpetersen.musicbot.api.plugin.management

import kotlin.reflect.KClass
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester

/**
 * Storage class for configured plugins that are (at least meant to be) active.
 *
 * @param defaultByBase the default plugin instance for each base
 * @param genericPlugins a list of all enabled generic plugins
 * @param playbackFactories a list of all enabled playback factories
 * @param providers a list of all enabled providers
 * @param suggesters a list of all enabled suggesters
 */
class PluginFinder(
    private val defaultByBase: Map<KClass<out Plugin>, Plugin>,
    val genericPlugins: List<GenericPlugin>,
    val playbackFactories: List<PlaybackFactory>,
    val providers: List<Provider>,
    val suggesters: List<Suggester>
) {

    /**
     * All bases for which a plugin is configured.
     */
    val defaultBases: Set<KClass<out Plugin>>
        get() = defaultByBase.keys

    /**
     * Gets the enabled plugin for the specified base type.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Plugin> get(base: KClass<T>): T? = defaultByBase[base] as T?

    /**
     * @return a sequence of all enabled plugins
     */
    fun allPlugins(): Sequence<Plugin> = sequenceOf(
        genericPlugins,
        playbackFactories,
        providers,
        suggesters
    ).flatMap { it.asSequence() }
}
