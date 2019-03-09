package net.bjoernpetersen.musicbot.internal.plugin

import net.bjoernpetersen.musicbot.api.plugin.NamedPlugin
import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.PluginLookup
import javax.inject.Inject
import javax.inject.Named
import kotlin.reflect.KClass

internal class PluginLookupImpl @Inject private constructor(
    @Named("PluginClassLoader")
    private val classLoader: ClassLoader,
    private val pluginFinder: PluginFinder
) : PluginLookup {

    override fun <T : Plugin> lookup(base: KClass<T>): T {
        val plugin = pluginFinder[base]
            ?: throw IllegalStateException(
                "Could not find provider for class ${base.qualifiedName}"
            )

        return try {
            plugin
        } catch (e: ClassCastException) {
            throw IllegalStateException()
        }
    }

    override fun <T : Plugin> lookup(plugin: NamedPlugin<T>): T {
        return lookup(plugin.id)
    }

    override fun <T : Plugin> lookup(id: String): T {
        val base = try {
            @Suppress("UNCHECKED_CAST")
            classLoader.loadClass(id).kotlin as KClass<T>
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("Could not find plugin class for ID: $id")
        } catch (e: ClassCastException) {
            throw IllegalStateException("Could not cast ID base to KClass<Plugin>")
        }
        return lookup(base)
    }
}
