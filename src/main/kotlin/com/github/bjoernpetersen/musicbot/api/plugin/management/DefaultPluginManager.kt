package com.github.bjoernpetersen.musicbot.api.plugin.management

import com.github.bjoernpetersen.musicbot.api.PluginLoader
import com.github.bjoernpetersen.musicbot.api.config.ChoiceBox
import com.github.bjoernpetersen.musicbot.api.config.Config
import com.github.bjoernpetersen.musicbot.api.config.ConfigSerializer
import com.github.bjoernpetersen.musicbot.api.config.NonnullConfigChecker
import com.github.bjoernpetersen.musicbot.api.config.SerializationException
import com.github.bjoernpetersen.musicbot.api.config.UiNode
import com.github.bjoernpetersen.musicbot.spi.plugin.Bases
import com.github.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import com.github.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import com.github.bjoernpetersen.musicbot.spi.plugin.Plugin
import com.github.bjoernpetersen.musicbot.spi.plugin.Provider
import com.github.bjoernpetersen.musicbot.spi.plugin.Suggester
import com.github.bjoernpetersen.musicbot.spi.plugin.management.ConfigurationException
import com.github.bjoernpetersen.musicbot.spi.plugin.management.PluginFinder
import com.github.bjoernpetersen.musicbot.spi.plugin.management.PluginManager
import mu.KotlinLogging
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class DefaultPluginManager(
    state: Config,
    override val genericPlugins: List<GenericPlugin>,
    override val playbackFactories: List<PlaybackFactory>,
    override val providers: List<Provider>,
    override val suggesters: List<Suggester>) : PluginManager {

    private val logger = KotlinLogging.logger { }

    private val allPlugins = genericPlugins + playbackFactories + providers + suggesters
    private val basesByPlugin: Map<Plugin, Set<KClass<out Plugin>>> = allPlugins.asSequence()
        .associateWith(::findBases)
    private val allBases: Set<KClass<out Plugin>> = basesByPlugin.values.flatten().toMutableSet()
        .apply { add(Suggester::class) }

    private val pluginSerializer = object : ConfigSerializer<Plugin> {
        override fun serialize(obj: Plugin): String = obj::class.qualifiedName!!

        override fun deserialize(string: String): Plugin = allPlugins
            .firstOrNull { it::class.qualifiedName == string } ?: throw SerializationException()
    }

    private val pluginByBase: Map<KClass<out Plugin>, Config.SerializedEntry<Plugin>> =
        allBases.associateWith { state.defaultEntry(it) }
    private val disabledByPlugin: Map<Plugin, Config.BooleanEntry> = allPlugins
        .associateWith { state.disabledEntry(it) }

    constructor(config: Config, pluginFolder: File) : this(config, loadPlugins(pluginFolder))

    private constructor(config: Config, plugins: Plugins) : this(
        config,
        plugins.generic,
        plugins.playbackFactories,
        plugins.providers,
        plugins.suggesters)

    override fun getBases(plugin: Plugin): Map<KClass<out Plugin>, Boolean> {
        return basesByPlugin[plugin]
            ?.associateWith { pluginByBase[it] == plugin } ?: throw IllegalStateException()
    }

    override fun <B : Plugin> getEnabled(base: KClass<out B>): B? {
        @Suppress("UNCHECKED_CAST")
        return pluginByBase[base]?.get() as B?
    }

    override fun <B : Plugin, P : B> isEnabled(plugin: P, base: KClass<out B>): Boolean {
        return pluginByBase[base]?.get() == plugin
    }

    override fun <B : Plugin, P : B> setEnabled(plugin: P, base: KClass<out B>) {
        pluginByBase[base]?.set(plugin) ?: logger.warn { "Tried to set default on unknown base" }
    }

    override fun setDisabled(base: KClass<out Plugin>) {
        pluginByBase[base]?.set(null) ?: logger.warn { "Tried to set default on unknown base" }
    }

    override fun isEnabled(plugin: Plugin): Boolean {
        val disabled = disabledByPlugin[plugin]?.get()
        if (disabled == null) {
            logger.warn {
                "Tried to get disabled state of unknown plugin"
            }
            return false
        }
        return !disabled
    }

    override fun setEnabled(plugin: Plugin) {
        disabledByPlugin[plugin]?.set(false) ?: logger.warn { "Tried to enable unknown plugin" }
    }

    override fun setDisabled(plugin: Plugin) {
        disabledByPlugin[plugin]?.set(true) ?: logger.warn { "Tried to disable unknown plugin" }
    }

    @Throws(ConfigurationException::class)
    override fun finish(): PluginFinder {
        val genericPlugins: List<GenericPlugin> = genericPlugins.filter(::isEnabled)
        val playbackFactories: List<PlaybackFactory> = playbackFactories.filter(::isEnabled)
        val providers: List<Provider> = providers.filter(::isEnabled)
        val suggesters: List<Suggester> = suggesters.filter(::isEnabled)

        val dependencies = sequenceOf(genericPlugins, playbackFactories, providers, suggesters)
            .flatMap { it.asSequence() }
            .flatMap {
                DependencyFinder.findDependencies(it).asSequence()
            }
        val defaultByBase = sequenceOf(dependencies, sequenceOf(Suggester::class))
            .flatMap { it }
            .distinct()
            .map {
                it to try {
                    getEnabled(it)
                } catch (e: SerializationException) {
                    null
                }
            }
            .filter {
                if (it.second == null) it.first != Suggester::class
                else true
            }
            .associate { (base, plugin) ->
                if (plugin == null)
                    throw ConfigurationException("No default: ${base.qualifiedName}")

                if (!isEnabled(plugin))
                    throw ConfigurationException(
                        "Default plugin for base ${base.qualifiedName} not enabled: ${plugin.name}")

                base to plugin
            }

        return PluginFinder(defaultByBase, genericPlugins, playbackFactories, providers, suggesters)
    }

    private fun Config.disabledEntry(plugin: Plugin): Config.BooleanEntry {
        val key = "${plugin::class.qualifiedName!!}.disabled"
        return BooleanEntry(key, "", false)
    }

    private fun Config.defaultEntry(base: KClass<out Plugin>): Config.SerializedEntry<Plugin> {
        fun defaultEntryUi(): UiNode<Plugin> {
            return ChoiceBox(
                { it.name },
                {
                    basesByPlugin.asSequence()
                        .filter { it.value.contains(base) }
                        .map { it.key }
                        .filter { isEnabled(it) }
                        .toList()
                })
        }

        val key = "${base.qualifiedName!!}.default"

        return SerializedEntry(
            key,
            "",
            pluginSerializer,
            NonnullConfigChecker,
            defaultEntryUi())
    }

    private companion object {
        fun loadPlugins(pluginFolder: File): Plugins {
            val loader = PluginLoader(pluginFolder)
            return Plugins(
                generic = loader.load(GenericPlugin::class).toList(),
                playbackFactories = loader.load(PlaybackFactory::class).toList(),
                providers = loader.load(Provider::class).toList(),
                suggesters = loader.load(Suggester::class).toList())
        }
    }
}

private fun findBases(plugin: Plugin): Set<KClass<out Plugin>> {
    val bases = plugin::class.findAnnotation<Bases>()
    return bases?.baseClasses?.toSet() ?: emptySet()
}

data class Plugins(
    val generic: List<GenericPlugin>,
    val playbackFactories: List<PlaybackFactory>,
    val providers: List<Provider>,
    val suggesters: List<Suggester>)
