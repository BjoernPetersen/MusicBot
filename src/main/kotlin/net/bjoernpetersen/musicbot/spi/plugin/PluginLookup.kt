package net.bjoernpetersen.musicbot.spi.plugin

import net.bjoernpetersen.musicbot.api.plugin.NamedPlugin
import kotlin.reflect.KClass

interface PluginLookup {
    fun <T : Plugin> lookup(base: Class<T>): T? = lookup(base.kotlin)
    fun <T : Plugin> lookup(base: KClass<T>): T?
    fun <T : Plugin> lookup(id: String): T?
    fun <T : Plugin> lookup(plugin: NamedPlugin<T>): T? = lookup(plugin.id)
}
