package net.bjoernpetersen.musicbot.api.plugin

import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.management.DependencyManager

/**
 * Represents one of the broader plugin categories.
 *
 * All plugins (indirectly) implement one specialized plugin sub-interface. Those sub-interfaces are
 * called categories and this enum contains some helper/accessor methods to work with them in a
 * generalized manner.
 *
 * For example, the [DependencyManager] interface has methods to retrieve plugins from each
 * category. You are creating a MusicBot implementation and want to show a tab per category
 * containing a list of all plugins in that category. For that purpose you could create a tab for
 * each item in this this enum and use the [select] method to call the right accessor function.
 *
 * @param type the interface class
 */
enum class PluginCategory(val type: KClass<out Plugin>) {
    GENERIC(GenericPlugin::class) {
        override fun select(dependencyManager: DependencyManager): List<Plugin> {
            return dependencyManager.genericPlugins
        }

        override fun select(finder: PluginFinder): List<Plugin> {
            return finder.genericPlugins
        }
    },
    PLAYBACK_FACTORY(PlaybackFactory::class) {
        override fun select(dependencyManager: DependencyManager): List<Plugin> {
            return dependencyManager.playbackFactories
        }

        override fun select(finder: PluginFinder): List<Plugin> {
            return finder.playbackFactories
        }
    },
    PROVIDER(Provider::class) {
        override fun select(dependencyManager: DependencyManager): List<Plugin> {
            return dependencyManager.providers
        }

        override fun select(finder: PluginFinder): List<Plugin> {
            return finder.providers
        }
    },
    SUGGESTER(Suggester::class) {
        override fun select(dependencyManager: DependencyManager): List<Plugin> {
            return dependencyManager.suggesters
        }

        override fun select(finder: PluginFinder): List<Plugin> {
            return finder.suggesters
        }
    };

    /**
     * A simple name of the category.
     */
    val simpleName = type.simpleName!!

    /**
     * @param dependencyManager a dependency manager
     * @return all plugins in this category from the dependency manager
     */
    abstract fun select(dependencyManager: DependencyManager): List<Plugin>

    /**
     * @param finder a plugin finder
     * @return all plugins in this category from the plugin finder
     */
    abstract fun select(finder: PluginFinder): List<Plugin>

    override fun toString(): String = simpleName

    companion object {
        /**
         * @param type a plugin class
         * @return the category of the plugin
         * @throws IllegalArgumentException if [type] doesn't extend a known category
         */
        operator fun invoke(type: KClass<*>): PluginCategory {
            return values().firstOrNull { it.type.isSuperclassOf(type) }
                ?: throw IllegalArgumentException(
                    "Does not extend a known category: ${type.qualifiedName}"
                )
        }

        /**
         * @param plugin a plugin
         * @return the category of the plugin
         * @throws IllegalArgumentException if [plugin] doesn't extend a known category
         */
        operator fun invoke(plugin: Plugin): PluginCategory {
            return when (plugin) {
                is GenericPlugin -> GENERIC
                is PlaybackFactory -> PLAYBACK_FACTORY
                is Provider -> PROVIDER
                is Suggester -> SUGGESTER
                else -> throw IllegalArgumentException(
                    "Does not extend a known category: ${plugin::class.qualifiedName}"
                )
            }
        }
    }
}

/**
 * Assumes this class is a plugin class and retrieves its [category][PluginCategory].
 */
val KClass<*>.pluginCategory: PluginCategory
    get() = PluginCategory(this)

/**
 * Gets the category of this plugin.
 */
val Plugin.category: PluginCategory
    get() = PluginCategory(this)
