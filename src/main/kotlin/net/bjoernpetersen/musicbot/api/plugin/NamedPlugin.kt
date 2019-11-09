package net.bjoernpetersen.musicbot.api.plugin

import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.UserFacing
import kotlin.reflect.KClass

/**
 * Static, serializable representation of a [user-facing][UserFacing] plugin.
 *
 * @param id the qualified name of the plugin's [ID base][IdBase]
 * @param name the plugin's [subject][UserFacing.subject]
 */
data class NamedPlugin<out T>(
    val id: String,
    val name: String
) where T : Plugin, T : UserFacing {

    /**
     * Convenience constructor to create an instance using the ID base class.
     *
     * @param idClass ID base class
     * @param name the plugin's [subject][UserFacing.subject]
     */
    constructor(idClass: KClass<out T>, name: String) : this(idClass.java.name, name)

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

/**
 * Creates a NamedPlugin for this plugin instance. Only works for active plugins (with ID).
 */
fun <T> T.toNamedPlugin(): NamedPlugin<T> where T : Plugin, T : UserFacing {
    val id = id.qualifiedName
    val subject = subject
    return NamedPlugin(id = id, name = subject)
}
