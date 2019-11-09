package net.bjoernpetersen.musicbot.api.plugin

import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.management.DependencyManager
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

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

    val simpleName = type.simpleName!!

    abstract fun select(dependencyManager: DependencyManager): List<Plugin>
    abstract fun select(finder: PluginFinder): List<Plugin>
    override fun toString(): String = simpleName

    companion object {
        operator fun invoke(type: KClass<*>): PluginCategory {
            return values().firstOrNull { it.type.isSuperclassOf(type) }
                ?: throw IllegalArgumentException(
                    "Does not extend a known category: ${type.qualifiedName}"
                )
        }

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

val KClass<*>.pluginCategory: PluginCategory
    get() = PluginCategory(this)

val Plugin.category: PluginCategory
    get() = PluginCategory(this)
