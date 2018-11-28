package net.bjoernpetersen.musicbot.api.module

import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.management.PluginFinder
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import kotlin.reflect.KClass

class PluginModule(private val pluginFinder: PluginFinder) : AbstractModule() {
    private fun configureDefaults() {
        pluginFinder.defaultBases.forEach { base ->
            base as KClass<Plugin>
            pluginFinder[base]?.apply {
                bind(base.java).toProvider(javax.inject.Provider { this })
            }
        }
    }

    private fun configureAll(base: KClass<out Plugin>, plugins: Collection<Plugin>) {
        base as KClass<Plugin>
        Multibinder.newSetBinder(binder(), base.java).apply {
            plugins.forEach {
                addBinding().toProvider(javax.inject.Provider { it })
            }
        }
    }

    override fun configure() {
        bind(PluginFinder::class.java).toInstance(pluginFinder)
        configureDefaults()
        configureAll(Plugin::class, pluginFinder.genericPlugins)
        configureAll(PlaybackFactory::class, pluginFinder.playbackFactories)
        configureAll(Provider::class, pluginFinder.providers)
        configureAll(Suggester::class, pluginFinder.suggesters)
    }
}
