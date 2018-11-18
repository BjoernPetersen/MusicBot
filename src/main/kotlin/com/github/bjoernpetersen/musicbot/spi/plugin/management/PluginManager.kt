package com.github.bjoernpetersen.musicbot.spi.plugin.management

import com.github.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import com.github.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import com.github.bjoernpetersen.musicbot.spi.plugin.Plugin
import com.github.bjoernpetersen.musicbot.spi.plugin.Provider
import com.github.bjoernpetersen.musicbot.spi.plugin.Suggester
import kotlin.reflect.KClass

interface PluginManager {
    val genericPlugins: List<GenericPlugin>
    val playbackFactories: List<PlaybackFactory>
    val providers: List<Provider>
    val suggesters: List<Suggester>

    fun getBases(plugin: Plugin): Map<KClass<out Plugin>, Boolean>

    fun <B : Plugin> getEnabled(base: KClass<B>): B?
    fun <B : Plugin, P : B> isEnabled(plugin: P, base: KClass<B>): Boolean
    fun <B : Plugin, P : B> setEnabled(plugin: P, base: KClass<B>)
    fun <B : Plugin, P : B> setDisabled(plugin: P, base: KClass<B>)
    fun isEnabled(plugin: Plugin): Boolean
    fun setEnabled(plugin: Plugin)
    fun setDisabled(plugin: Plugin)

    @Throws(ConfigurationException::class)
    fun finish(): PluginFinder
}

class ConfigurationException(message: String) : Exception(message)
