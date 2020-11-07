package net.bjoernpetersen.musicbot.api.plugin

import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

/**
 * An exception during plugin initialization.
 */
open class InitializationException : Exception {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * An exception thrown by Plugins if they are misconfigured.
 */
class ConfigurationException : InitializationException {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * Thrown if a plugin's declaration is invalid.
 */
class DeclarationException : RuntimeException {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

private val KClass<*>.isBase: Boolean
    get() = isBase()

private const val BASE_SEARCH_DEPTH_CAPACITY = 20

private fun KClass<*>.isBase(
    visited: MutableSet<KClass<*>> = HashSet(BASE_SEARCH_DEPTH_CAPACITY)
): Boolean {
    if (!visited.add(this)) return false
    return findAnnotation<Base>() != null ||
        annotations.any { it.annotationClass.isBase(visited) }
}

val KClass<*>.pluginBases: List<KClass<out Plugin>>
    get() {
        val specs = mutableListOf<KClass<out Plugin>>()
        if (this.isBase) specs.add(this as KClass<out Plugin>)
        this.allSuperclasses.asSequence()
            .filter { it.isBase }
            .filter {
                it.isSubclassOf(Plugin::class).also { isSubclass ->
                    if (!isSubclass) {
                        throw DeclarationException(
                            "Base ${it.qualifiedName} is not a plugin subtype"
                        )
                    }
                }
            }
            .map {
                @Suppress("UNCHECKED_CAST")
                it as KClass<out Plugin>
            }
            .forEach { specs.add(it) }

        return specs
    }
@Deprecated("Renamed to pluginBases", ReplaceWith("pluginBases"))
val KClass<*>.bases: List<KClass<out Plugin>>
    get() = pluginBases

val Plugin.bases: List<KClass<out Plugin>>
    get() = this::class.pluginBases
