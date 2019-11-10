package net.bjoernpetersen.musicbot.api.plugin.management

import com.google.common.collect.MultimapBuilder
import kotlin.reflect.KClass
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.config.ChoiceBox
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.config.ConfigSerializer
import net.bjoernpetersen.musicbot.api.config.DeserializationException
import net.bjoernpetersen.musicbot.api.config.MainConfigScope
import net.bjoernpetersen.musicbot.api.config.NonnullConfigChecker
import net.bjoernpetersen.musicbot.api.config.UiNode
import net.bjoernpetersen.musicbot.api.plugin.DeclarationException
import net.bjoernpetersen.musicbot.api.plugin.PluginId
import net.bjoernpetersen.musicbot.api.plugin.PluginLoader
import net.bjoernpetersen.musicbot.api.plugin.bases
import net.bjoernpetersen.musicbot.api.plugin.id
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.management.DependencyConfigurationException
import net.bjoernpetersen.musicbot.spi.plugin.management.DependencyManager

/**
 * Default implementation of the [DependencyManager] interface.
 *
 * @param state a state config (usually from the [main config scope][MainConfigScope])
 * @param genericPlugins a list of all available generic plugins
 * @param playbackFactories a list of all available playback factories
 * @param providers a list of all available providers
 * @param suggesters a list of all available suggesters
 */
class DefaultDependencyManager(
    state: Config,
    override val genericPlugins: List<GenericPlugin>,
    override val playbackFactories: List<PlaybackFactory>,
    override val providers: List<Provider>,
    override val suggesters: List<Suggester>
) : DependencyManager {

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
            .firstOrNull { it::class.qualifiedName == string } ?: throw DeserializationException()
    }

    private val defaultByBase: Map<KClass<out Plugin>, Config.SerializedEntry<Plugin>> =
        allBases.associateWith { state.defaultEntry(it) }

    /**
     * Convenience constructor which loads all plugins using the specified PluginLoader.
     *
     * @param state the [MainConfigScope] state config
     * @param loader a plugin loader
     */
    constructor(state: Config, loader: PluginLoader) : this(state, loadPlugins(loader))

    private constructor(state: Config, plugins: Plugins) : this(
        state,
        plugins.generic,
        plugins.playbackFactories,
        plugins.providers,
        plugins.suggesters
    )

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
    override fun finish(
        providerOrder: List<PluginId>,
        suggesterOrder: List<PluginId>
    ): PluginFinder {
        val genericPlugins: List<GenericPlugin> = findEnabledGeneric()
        val playbackFactories: List<PlaybackFactory> = findEnabledPlaybackFactory()
        val providers: List<Provider> = findEnabledProvider().sortedByOrder(providerOrder)
        val suggesters: List<Suggester> = findEnabledSuggester().sortedByOrder(suggesterOrder)

        val defaultByBase = findEnabledDependencies()
            .associateWithTo(HashMap()) { base ->
                val plugin = try {
                    getDefault(base)
                } catch (e: DeserializationException) {
                    null
                } ?: throw DependencyConfigurationException("No default: ${base.qualifiedName}")
                plugin
            }

        sequenceOf(genericPlugins, playbackFactories, providers, suggesters)
            .flatMap { it.asSequence() }
            .forEach {
                try {
                    defaultByBase[it.id.type] = it
                } catch (e: DeclarationException) {
                    // ignore
                }
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
            defaultEntryUi()
        )
    }

    private companion object {
        /**
         * Loads plugins of all types using the specified loader.
         *
         * @param loader a plugin loader
         * @return all loaded plugins
         */
        fun loadPlugins(loader: PluginLoader): Plugins {
            return Plugins(
                generic = loader.load(GenericPlugin::class).toList(),
                playbackFactories = loader.load(PlaybackFactory::class).toList(),
                providers = loader.load(Provider::class).toList(),
                suggesters = loader.load(Suggester::class).toList()
            )
        }
    }
}

/**
 * A data class containing all plugins which have been loaded.
 *
 * @param generic all available generic plugins
 * @param playbackFactories all available playback factories
 * @param providers all available providers
 * @param suggesters all available suggesters
 */
data class Plugins(
    val generic: List<GenericPlugin>,
    val playbackFactories: List<PlaybackFactory>,
    val providers: List<Provider>,
    val suggesters: List<Suggester>
)

private fun <T : Plugin> List<T>.sortedByOrder(order: List<PluginId>): List<T> {
    return sortedBy { order.indexOf(it.id) }
}
