package net.bjoernpetersen.musicbot.spi.plugin

import java.lang.annotation.Inherited
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Annotation for plugins to declare their base classes or interfaces.
 *
 * The base classes will typically be at least the specialized plugin interface
 * (e.g. [Provider] or [Suggester]) and a base class or interface for the concrete
 * plugin, may also be the class itself.
 */
@Inherited
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Bases(vararg val baseClasses: KClass<out Plugin>)

class MissingBasesException(type: KClass<*>) : RuntimeException(type.qualifiedName)

val Plugin.bases
    get() = this::class.bases

val KClass<out Plugin>.bases: List<KClass<out Plugin>>
    get() {
        return findAnnotation<Bases>()?.baseClasses?.toList() ?: throw MissingBasesException(this)
    }
