package net.bjoernpetersen.musicbot.spi.plugin.management

import com.google.common.annotations.Beta
import net.bjoernpetersen.musicbot.api.plugin.management.findDependencies
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.bases
import net.bjoernpetersen.musicbot.spi.plugin.id
import net.bjoernpetersen.musicbot.spi.plugin.isActive
import kotlin.reflect.KClass

/**
 * Manages dependency configuration.
 */
interface DependencyManager {

    val genericPlugins: List<GenericPlugin>
    val playbackFactories: List<PlaybackFactory>
    val providers: List<Provider>
    val suggesters: List<Suggester>
    val allPlugins: Sequence<Plugin>
        get() = sequenceOf(
            genericPlugins,
            playbackFactories,
            providers,
            suggesters)
            .flatMap { it.asSequence() }

    fun getDefaults(plugin: Plugin): Map<KClass<out Plugin>, Boolean>

    fun <B : Plugin> getDefault(base: KClass<out B>): B?
    fun isDefault(plugin: Plugin, base: KClass<*>): Boolean
    fun setDefault(plugin: Plugin?, base: KClass<*>)

    fun isActive(plugin: Plugin): Boolean {
        return when (plugin) {
            is GenericPlugin, is PlaybackFactory -> plugin.bases.any { isDefault(plugin, it) }
            else -> isDefault(plugin, plugin.id)
        }
    }

    fun findAvailable(base: KClass<*>): List<Plugin>

    fun findActiveGeneric(): List<GenericPlugin> = genericPlugins.filter(::isActive)
    fun findActivePlaybackFactory(): List<PlaybackFactory> = playbackFactories.filter(::isActive)
    fun findActiveProvider(): List<Provider> = providers.filter(::isActive)
    fun findActiveSuggester(): List<Suggester> = suggesters.filter(::isActive)

    @Beta
    fun findActiveDependencies(): Set<KClass<out Plugin>> {
        val result = allPlugins
            .filter(::isActive)
            .flatMap { it.findDependencies().asSequence() }
            .toMutableSet()
        allPlugins
            .filter { isActive(it) }
            .map { getDefaults(it) }
            .flatMap { it.asSequence() }
            .filter { it.value }
            .map { it.key }
            .forEach { result.add(it) }
        // TODO remove the following?
        allPlugins
            .filter { it::class.isActive }
            .map { it.id }
            .forEach { result.add(it) }
        return result
    }

    @Throws(DependencyConfigurationException::class)
    fun finish(): PluginFinder
}

class DependencyConfigurationException(message: String) : Exception(message)
