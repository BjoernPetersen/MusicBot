package net.bjoernpetersen.musicbot.api.plugin.management

import com.google.inject.spi.InjectionPoint
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun Plugin.findDependencies(): Set<KClass<out Plugin>> {
    return InjectionPoint.forInstanceMethodsAndFields(this::class.java)
        .flatMap { it.dependencies }
        .map { it.key }
        .map { it.typeLiteral.rawType }
        .map { it as Class<out Any> }
        .map { it.kotlin }
        .filter { it.isSubclassOf(Plugin::class) }
        .map { it as KClass<out Plugin> }
        .toSet()
}

