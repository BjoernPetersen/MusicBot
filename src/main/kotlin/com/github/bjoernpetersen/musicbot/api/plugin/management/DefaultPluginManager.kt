package com.github.bjoernpetersen.musicbot.api.plugin.management

import com.github.bjoernpetersen.musicbot.api.PluginLoader
import com.github.bjoernpetersen.musicbot.api.config.ChoiceBox
import com.github.bjoernpetersen.musicbot.api.config.Config
import com.github.bjoernpetersen.musicbot.api.config.ConfigSerializer
import com.github.bjoernpetersen.musicbot.api.config.NonnullConfigChecker
import com.github.bjoernpetersen.musicbot.api.config.SerializationException
import com.github.bjoernpetersen.musicbot.api.config.UiNode
import com.github.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import com.github.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import com.github.bjoernpetersen.musicbot.spi.plugin.Plugin
import com.github.bjoernpetersen.musicbot.spi.plugin.Provider
import com.github.bjoernpetersen.musicbot.spi.plugin.Suggester
import com.github.bjoernpetersen.musicbot.spi.plugin.bases
import com.github.bjoernpetersen.musicbot.spi.plugin.id
import com.github.bjoernpetersen.musicbot.spi.plugin.management.ConfigurationException
import com.github.bjoernpetersen.musicbot.spi.plugin.management.PluginFinder
import com.github.bjoernpetersen.musicbot.spi.plugin.management.PluginManager
import com.google.common.collect.MultimapBuilder
import mu.KotlinLogging
import java.io.File
import kotlin.reflect.KClass

class DefaultPluginManager(
    state: Config,
    override val genericPlugins: List<GenericPlugin>,
    override val playbackFactories: List<PlaybackFactory>,
    override val providers: List<Provider>,
    override val suggesters: List<Suggester>) : PluginManager {

    private val logger = KotlinLogging.logger { }

    private val allPlugins = genericPlugins + playbackFactories + providers + suggesters
    private val basesByPlugin: Map<Plugin, Set<KClass<out Plugin>>> = allPlugins
        .associateWith { it.bases.toSet() }
    private val allBases: Set<KClass<out Plugin>> = basesByPlugin.values.flatten().toSet()

    private val pluginSerializer = object : ConfigSerializer<Plugin> {
        override fun serialize(obj: Plugin): String = obj::class.qualifiedName!!

        override fun deserialize(string: String): Plugin = allPlugins
            .firstOrNull { it::class.qualifiedName == string } ?: throw SerializationException()
    }

    private val defaultByBase: Map<KClass<out Plugin>, Config.SerializedEntry<Plugin>> =
        allBases.associateWith { state.defaultEntry(it) }

    constructor(config: Config, pluginFolder: File) : this(config, loadPlugins(pluginFolder))

    private constructor(config: Config, plugins: Plugins) : this(
        config,
        plugins.generic,
        plugins.playbackFactories,
        plugins.providers,
        plugins.suggesters)

    override fun getDefaults(plugin: Plugin): Map<KClass<out Plugin>, Boolean> {
        return basesByPlugin[plugin]?.associateWith { defaultByBase[it] == plugin }
            ?: throw IllegalStateException()
    }

    override fun <B : Plugin> getDefault(base: KClass<out B>): B? {
        @Suppress("UNCHECKED_CAST")
        return defaultByBase[base]?.get() as B?
    }

    override fun <B : Plugin, P : B> isDefault(plugin: P, base: KClass<out B>): Boolean {
        return defaultByBase[base]?.get() == plugin
    }

    override fun <B : Plugin, P : B> setDefault(plugin: P, base: KClass<out B>) {
        defaultByBase[base]?.set(plugin) ?: logger.warn { "Tried to set default on unknown base" }
    }

    private fun isEnabled(plugin: Plugin): Boolean {
        return isDefault(plugin, plugin.id)
    }

    @Throws(ConfigurationException::class)
    override fun finish(): PluginFinder {

        val genericPlugins: List<GenericPlugin> = genericPlugins.filter(::isEnabled)
        val playbackFactories: List<PlaybackFactory> = playbackFactories.filter(::isEnabled)
        val providers: List<Provider> = providers.filter(::isEnabled)
        val suggesters: List<Suggester> = suggesters.filter(::isEnabled)

        val defaultByBase = sequenceOf(genericPlugins, playbackFactories, providers, suggesters)
            .flatMap { it.asSequence() }
            .flatMap {
                DependencyFinder.findDependencies(it).asSequence()
            }
            .distinct()
            .associateWith { base ->
                val plugin = try {
                    getDefault(base)
                } catch (e: SerializationException) {
                    null
                } ?: throw ConfigurationException("No default: ${base.qualifiedName}")

                if (!isEnabled(plugin))
                    throw ConfigurationException(
                        "Default plugin for base ${base.qualifiedName} not enabled: ${plugin.name}")

                plugin
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

data class Plugins(
    val generic: List<GenericPlugin>,
    val playbackFactories: List<PlaybackFactory>,
    val providers: List<Provider>,
    val suggesters: List<Suggester>)
