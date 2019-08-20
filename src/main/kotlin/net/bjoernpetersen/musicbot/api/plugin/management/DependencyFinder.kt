package net.bjoernpetersen.musicbot.api.plugin.management

import com.google.inject.spi.InjectionPoint
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Find the immediate plugin dependencies of a plugin.
 *
 * This does not include any dependencies that do not extend [Plugin] since those don't need to be
 * enabled or disabled.
 */
fun Plugin.findDependencies(): Set<KClass<out Plugin>> {
    return InjectionPoint.forInstanceMethodsAndFields(this::class.java)
        .asSequence()
        .flatMap { it.dependencies.asSequence() }
        .map { it.key }
        .map { it.typeLiteral.rawType }
        .map { it.kotlin }
        .filter { it.isSubclassOf(Plugin::class) }
        .map {
            @Suppress("UNCHECKED_CAST")
            it as KClass<out Plugin>
        }
        .toSet()
}
