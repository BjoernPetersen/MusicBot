package net.bjoernpetersen.musicbot.spi.plugin

import net.bjoernpetersen.musicbot.api.plugin.ActiveBase
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.api.plugin.NamedPlugin
import kotlin.reflect.KClass

/**
 * An object which can be used to look up plugins by its base or ID representation.
 */
interface PluginLookup {
    /**
     * Looks up the default plugin for the specified base.
     *
     * @param base the base for which the default plugin should be looked up
     * @return the default plugin, or null if there is none
     */
    fun <T : Plugin> lookup(base: Class<T>): T? = lookup(base.kotlin)

    /**
     * Looks up the default plugin for the specified base.
     *
     * @param base the base for which the default plugin should be looked up
     * @return the default plugin, or null if there is none
     */
    fun <T : Plugin> lookup(base: KClass<T>): T?

    /**
     * Looks up the default [active][ActiveBase] plugin for the specified [ID][IdBase].
     *
     * @param id a plugin ID
     * @return the plugin, or null if there is no default plugin for that ID
     */
    fun <T : Plugin> lookup(id: String): T?

    /**
     * Looks up the specified plugin by its ID.
     *
     * @param plugin the plugin specification
     * @return the plugin instance, or null if it can't be found
     */
    fun <T : Plugin> lookup(plugin: NamedPlugin<T>): T? = lookup(plugin.id)
}
