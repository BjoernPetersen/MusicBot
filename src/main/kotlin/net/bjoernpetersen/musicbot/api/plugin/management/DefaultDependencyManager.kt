package net.bjoernpetersen.musicbot.api.plugin.management

import com.google.common.collect.MultimapBuilder
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.plugin.PluginLoader
import net.bjoernpetersen.musicbot.api.config.ChoiceBox
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.config.ConfigSerializer
import net.bjoernpetersen.musicbot.api.config.NonnullConfigChecker
import net.bjoernpetersen.musicbot.api.config.SerializationException
import net.bjoernpetersen.musicbot.api.config.UiNode
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.bases
import net.bjoernpetersen.musicbot.spi.plugin.management.DependencyConfigurationException
import net.bjoernpetersen.musicbot.spi.plugin.management.DependencyManager
import kotlin.reflect.KClass

class DefaultDependencyManager(
    state: Config,
    override val genericPlugins: List<GenericPlugin>,
    override val playbackFactories: List<PlaybackFactory>,
    override val providers: List<Provider>,
    override val suggesters: List<Suggester>) : DependencyManager {

    private val logger = KotlinLogging.logger { }

    private val basesByPlugin: Map<Plugin, Set<KClass<out Plugin>>> = allPlugins
        .associateWith { it.bases.toSet() }
    private val allBases: Set<KClass<out Plugin>> = basesByPlugin.values.flatten().toSet()
    @Suppress("UnstableApiUsage")
    private val pluginsByBase = MultimapBuilder.SetMultimapBuilder
        .hashKeys()
        .hashSetValues()
        .build<KClass<*>, Plugin>().apply {
            for (entry in basesByPlugin) {
                entry.value.forEach {
                    put(it, entry.key)
                }
            }
        }

    private val pluginSerializer = object : ConfigSerializer<Plugin> {
        override fun serialize(obj: Plugin): String = obj::class.qualifiedName!!

        override fun deserialize(string: String): Plugin = allPlugins
            .firstOrNull { it::class.qualifiedName == string } ?: throw SerializationException()
    }

    private val defaultByBase: Map<KClass<out Plugin>, Config.SerializedEntry<Plugin>> =
        allBases.associateWith { state.defaultEntry(it) }

    constructor(config: Config, loader: PluginLoader) : this(config, loadPlugins(loader))

    private constructor(config: Config, plugins: Plugins) : this(
        config,
        plugins.generic,
        plugins.playbackFactories,
        plugins.providers,
        plugins.suggesters)

    override fun getDefaults(plugin: Plugin): Sequence<KClass<out Plugin>> {
        return basesByPlugin[plugin]?.asSequence()?.filter { defaultByBase[it]?.get() == plugin }
            ?: throw IllegalStateException()
    }

    override fun <B : Plugin> getDefault(base: KClass<out B>): B? {
        @Suppress("UNCHECKED_CAST")
        return defaultByBase[base]?.get() as B?
    }

    override fun isDefault(plugin: Plugin, base: KClass<*>): Boolean {
        return defaultByBase[base]?.get() == plugin
    }

    override fun setDefault(plugin: Plugin?, base: KClass<*>) {
        defaultByBase[base]?.set(plugin) ?: logger.warn { "Tried to set default on unknown base" }
    }

    override fun findAvailable(base: KClass<*>): List<Plugin> {
        return pluginsByBase[base].toList()
    }

    @Throws(DependencyConfigurationException::class)
    override fun finish(): PluginFinder {
        val genericPlugins: List<GenericPlugin> = findEnabledGeneric()
        val playbackFactories: List<PlaybackFactory> = findEnabledPlaybackFactory()
        val providers: List<Provider> = findEnabledProvider()
        val suggesters: List<Suggester> = findEnabledSuggester()

        val defaultByBase = findEnabledDependencies()
            .associateWith { base ->
                val plugin = try {
                    getDefault(base)
                } catch (e: SerializationException) {
                    null
                } ?: throw DependencyConfigurationException("No default: ${base.qualifiedName}")
                plugin
            }

        return PluginFinder(defaultByBase, genericPlugins, playbackFactories, providers, suggesters)
    }

    private fun Config.defaultEntry(base: KClass<out Plugin>): Config.SerializedEntry<Plugin> {
        fun defaultEntryUi(): UiNode<Plugin> {
            return ChoiceBox(
                { it.name },
                { pluginsByBase[base].toList() })
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
        fun loadPlugins(loader: PluginLoader): Plugins {
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
