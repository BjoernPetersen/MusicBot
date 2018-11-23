package net.bjoernpetersen.musicbot.api.plugin.management

import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import com.google.inject.spi.InjectionPoint
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object DependencyFinder {
    fun findDependencies(plugin: Plugin): Set<KClass<out Plugin>> {
        return InjectionPoint.forInstanceMethodsAndFields(plugin::class.java)
            .flatMap { it.dependencies }
            .map { it.key }
            .map { it.typeLiteral.rawType }
            .map { it as Class<out Any> }
            .map { it.kotlin }
            .filter { it.isSubclassOf(Plugin::class) }
            .map { it as KClass<out Plugin> }
            .toSet()
    }
}
