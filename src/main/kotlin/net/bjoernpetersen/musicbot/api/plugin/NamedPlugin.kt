package net.bjoernpetersen.musicbot.api.plugin

import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import kotlin.reflect.KClass

data class NamedPlugin<out T : Plugin>(val id: String, val subject: String) {
    constructor(idClass: KClass<out T>, name: String) : this(idClass.java.name, name)

    @Throws(IllegalStateException::class)
    fun findPlugin(classLoader: ClassLoader, pluginFinder: PluginFinder): T {
        val base = try {
            @Suppress("UNCHECKED_CAST")
            classLoader.loadClass(id).kotlin as KClass<Plugin>
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("Could not find plugin class for ID: $id")
        } catch (e: ClassCastException) {
            throw IllegalStateException("Could not cast ID base to KClass<Plugin>")
        }

        val plugin = pluginFinder[base]
            ?: throw IllegalStateException(
                "Could not find provider for class ${base.qualifiedName}")

        return try {
            @Suppress("UNCHECKED_CAST")
            plugin as T
        } catch (e: ClassCastException) {
            throw IllegalStateException()
        }
    }
}
