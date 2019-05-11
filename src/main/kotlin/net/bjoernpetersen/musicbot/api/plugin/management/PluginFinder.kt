package net.bjoernpetersen.musicbot.api.plugin.management

import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import kotlin.reflect.KClass

/**
 * Storage class for configured plugins that are (at least meant to be) active.
 */
class PluginFinder(
    private val defaultByBase: Map<KClass<out Plugin>, Plugin>,
    val genericPlugins: List<GenericPlugin>,
    val playbackFactories: List<PlaybackFactory>,
    val providers: List<Provider>,
    val suggesters: List<Suggester>
) {

    val defaultBases: Set<KClass<out Plugin>>
        get() = defaultByBase.keys

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Plugin> get(base: KClass<T>): T? = defaultByBase[base] as T?

    fun allPlugins(): Sequence<Plugin> = sequenceOf(
        genericPlugins,
        playbackFactories,
        providers,
        suggesters)
        .flatMap { it.asSequence() }
}
