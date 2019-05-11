package net.bjoernpetersen.musicbot.spi.plugin.management

import com.google.common.annotations.Beta
import net.bjoernpetersen.musicbot.api.plugin.ActiveBase
import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.api.plugin.management.findDependencies
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.bases
import net.bjoernpetersen.musicbot.spi.plugin.hasActiveBase
import net.bjoernpetersen.musicbot.spi.plugin.id
import kotlin.reflect.KClass

/**
 * Manages dependency configuration before injection.
 */
interface DependencyManager {

    /**
     * All loaded generic plugins.
     */
    val genericPlugins: List<GenericPlugin>

    /**
     * All loaded playback plugins.
     */
    val playbackFactories: List<PlaybackFactory>

    /**
     * All loaded Provider plugins.
     */
    val providers: List<Provider>

    /**
     * All loaded Suggester plugins.
     */
    val suggesters: List<Suggester>

    /**
     * Convenience accessor for a sequence of all loaded plugins.
     */
    val allPlugins: Sequence<Plugin>
        get() = sequenceOf(
            genericPlugins,
            playbackFactories,
            providers,
            suggesters)
            .flatMap { it.asSequence() }

    /**
     * Get the bases a plugin is set as the default for.
     *
     * @param plugin a plugin, must be contained in [allPlugins]
     */
    fun getDefaults(plugin: Plugin): Sequence<KClass<out Plugin>>

    /**
     * Get the plugin that is currently set as the default for the given base.
     *
     * Note that this will **not** return a list of plugins for active bases like [Provider].
     * For those cases the behavior of this method is undefined.
     *
     * @param base a base class
     * @return the default plugin, or null if none is set
     */
    fun <B : Plugin> getDefault(base: KClass<out B>): B?

    /**
     * Determines whether the given plugin is the default for the given base.
     *
     * @param plugin a plugin
     * @param base any base class
     * @return whether the plugin is the default
     */
    fun isDefault(plugin: Plugin, base: KClass<*>): Boolean

    /**
     * Set the given plugins as the default for the given base class.
     *
     * @param plugin, or null
     * @param base any base class
     */
    fun setDefault(plugin: Plugin?, base: KClass<*>)

    private fun isRequired(
        plugin: Plugin,
        visited: MutableMap<Plugin, Boolean?> = HashMap()
    ): Boolean {
        if (plugin::class.hasActiveBase && isDefault(plugin, plugin.id)) return true
        if (plugin in visited) return visited[plugin]
            ?: throw IllegalStateException("Cyclic dependency: $plugin $visited")
        visited[plugin] = null

        val bases = plugin.bases.asSequence()
            .filter { isDefault(plugin, it) }
            .toSet()

        // TODO: this is very expensive
        return allPlugins.asSequence()
            .filter { it !== plugin }
            .filter { !it.findDependencies().intersect(bases).isEmpty() }
            .any { isRequired(it, visited) }
            .also { visited[plugin] = it }
    }

    /**
     * Determines whether the given plugin is enabled.
     * A plugin is enabled, if the plugin is the default for its ID base
     * and one of these conditions is met:
     *
     * - The plugin is active, i.e. has an [active base][ActiveBase]
     * - Any enabled plugin (transitively) depends on the plugin's ID base
     *
     * @param plugin a plugin
     * @return whether the plugin is enabled
     */
    @Beta
    fun isEnabled(plugin: Plugin): Boolean {
        return isRequired(plugin)
    }

    /**
     * Finds all plugins implementing the given base class.
     */
    fun findAvailable(base: KClass<*>): List<Plugin>

    @Beta
    fun findEnabledGeneric(): List<GenericPlugin> = genericPlugins.filter(::isEnabled)

    @Beta
    fun findEnabledPlaybackFactory(): List<PlaybackFactory> = playbackFactories.filter(::isEnabled)

    @Beta
    fun findEnabledProvider(): List<Provider> = providers.filter(::isEnabled)

    @Beta
    fun findEnabledSuggester(): List<Suggester> = suggesters.filter(::isEnabled)

/*    @Beta
    fun findOptionalDependencies(): Set<KClass<out Plugin>> {
        return allPlugins
            .filter { it::class.isActivePlugin }
            .map { it.id }
            .toSet()
    }*/

    /**
     * Recursively find all dependencies of the currently enabled plugins.
     */
    @Beta
    fun findEnabledDependencies(): Set<KClass<out Plugin>> {
        return allPlugins
            .filter { isEnabled(it) }
            .flatMap { it.findDependencies().asSequence() }
            .toSet()
        /* val result = allPlugins
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
        return result*/
    }

    @Throws(DependencyConfigurationException::class)
    fun finish(): PluginFinder
}

class DependencyConfigurationException(message: String) : Exception(message)
