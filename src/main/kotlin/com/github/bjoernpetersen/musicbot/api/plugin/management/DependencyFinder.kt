package com.github.bjoernpetersen.musicbot.api.plugin.management

import com.github.bjoernpetersen.musicbot.spi.plugin.Plugin
import com.google.inject.spi.InjectionPoint
import kotlin.reflect.KClass

object DependencyFinder {
    fun findDependencies(plugin: Plugin): Set<KClass<out Any>> {
        return InjectionPoint.forInstanceMethodsAndFields(plugin::class.java)
            .flatMap { it.dependencies }
            .map { it.key }
            .map { it.typeLiteral.rawType }
            .map { it as Class<out Any> }
            .filter { Plugin::class.java.isAssignableFrom(it) }
            .map { it.kotlin }
            .toSet()
    }
}
