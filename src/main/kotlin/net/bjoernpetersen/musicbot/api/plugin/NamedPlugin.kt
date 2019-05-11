package net.bjoernpetersen.musicbot.api.plugin

import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.UserFacing
import kotlin.reflect.KClass

/**
 * Static, serializable representation of a plugin.
 *
 * @param id the qualified name of the plugin's [ID base][IdBase]
 * @param name the plugin's [subject][UserFacing.subject]
 */
data class NamedPlugin<out T : Plugin>(
    val id: String,
    val name: String
) {

    constructor(idClass: KClass<out T>, name: String) : this(idClass.java.name, name)

    @Deprecated("Use PluginLookup", ReplaceWith("PluginLookup.lookup(this)"))
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
                "Could not find provider for class ${base.qualifiedName}"
            )

        return try {
            @Suppress("UNCHECKED_CAST")
            plugin as T
        } catch (e: ClassCastException) {
            throw IllegalStateException()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NamedPlugin<*>) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
