package net.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import net.bjoernpetersen.musicbot.api.plugin.fix
import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.internal.plugin.PluginLookupImpl
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.PluginLookup
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import kotlin.reflect.KClass

class PluginModule(private val pluginFinder: PluginFinder) : AbstractModule() {
    private fun configureDefaults() {
        pluginFinder.defaultBases.forEach {
            it.fix().let { base ->
                pluginFinder[base]?.apply {
                    bind(base.java).toProvider(javax.inject.Provider { this })
                }
            }
        }
    }

    private fun configureAll(base: KClass<out Plugin>, plugins: Collection<Plugin>) {
        base.fix().let { fixedBase ->
            Multibinder.newSetBinder(binder(), fixedBase.java).apply {
                plugins.forEach {
                    addBinding().toProvider(javax.inject.Provider { it })
                }
            }
        }
    }

    override fun configure() {
        bind(PluginFinder::class.java).toInstance(pluginFinder)
        bind(PluginLookup::class.java).to(PluginLookupImpl::class.java)
        configureDefaults()
        configureAll(GenericPlugin::class, pluginFinder.genericPlugins)
        configureAll(PlaybackFactory::class, pluginFinder.playbackFactories)
        configureAll(Provider::class, pluginFinder.providers)
        configureAll(Suggester::class, pluginFinder.suggesters)
    }
}
