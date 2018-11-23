package net.bjoernpetersen.musicbot.spi.plugin.management

import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import kotlin.reflect.KClass

interface PluginManager {
    val genericPlugins: List<GenericPlugin>
    val playbackFactories: List<PlaybackFactory>
    val providers: List<Provider>
    val suggesters: List<Suggester>

    fun getDefaults(plugin: Plugin): Map<KClass<out Plugin>, Boolean>

    fun <B : Plugin> getDefault(base: KClass<out B>): B?
    fun <B : Plugin, P : B> isDefault(plugin: P, base: KClass<out B>): Boolean
    fun <B : Plugin, P : B> setDefault(plugin: P, base: KClass<out B>)

    @Throws(ConfigurationException::class)
    fun finish(): PluginFinder
}

class ConfigurationException(message: String) : Exception(message)
