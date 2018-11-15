package com.github.bjoernpetersen.musicbot.spi.plugin.management

import com.github.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import com.github.bjoernpetersen.musicbot.spi.plugin.Plugin
import com.github.bjoernpetersen.musicbot.spi.plugin.Provider
import com.github.bjoernpetersen.musicbot.spi.plugin.Suggester
import kotlin.reflect.KClass

/**
 * Storage class for configured plugins that are (at least meant to be) active.
 */
class PluginFinder(
    private val defaultByBase: Map<KClass<out Plugin>, Plugin>,
    val plugins: List<Plugin>,
    val playbackFactories: List<PlaybackFactory>,
    val providers: List<Provider>,
    val suggesters: List<Suggester>) {

    val defaultKeys: Set<KClass<out Plugin>>
        get() = defaultByBase.keys

    operator fun <T : Plugin> get(base: KClass<T>): T? = defaultByBase[base] as T?
}
