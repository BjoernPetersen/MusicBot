package net.bjoernpetersen.musicbot.internal.plugin

import javax.inject.Inject
import javax.inject.Named
import kotlin.reflect.KClass
import net.bjoernpetersen.musicbot.api.plugin.NamedPlugin
import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.PluginLookup
import net.bjoernpetersen.musicbot.spi.plugin.UserFacing

internal class PluginLookupImpl @Inject private constructor(
    @Named("PluginClassLoader")
    private val classLoader: ClassLoader,
    private val pluginFinder: PluginFinder
) : PluginLookup {

    override fun <T : Plugin> lookup(base: KClass<T>): T? {
        return pluginFinder[base]
    }

    override fun <T> lookup(plugin: NamedPlugin<T>): T? where T : Plugin, T : UserFacing {
        return lookup(plugin.id)
    }

    override fun <T : Plugin> lookup(id: String): T? {
        val base = try {
            @Suppress("UNCHECKED_CAST")
            classLoader.loadClass(id).kotlin as KClass<T>
        } catch (e: ClassNotFoundException) {
            null
        } catch (e: ClassCastException) {
            null
        }
        return base?.let(::lookup)
    }
}
