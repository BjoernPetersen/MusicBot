package net.bjoernpetersen.musicbot.spi.plugin

import java.lang.annotation.Inherited
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Specifies the most specific base for the plugin. The base must still be included in the [Bases].
 */
@Inherited
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class IdBase(val baseClass: KClass<out Plugin>)

class MissingIdBaseException(type: KClass<*>) : RuntimeException(type.qualifiedName)

val Plugin.id
    get() = this::class.id

val KClass<out Plugin>.id: KClass<out Plugin>
    get() {
        return findAnnotation<IdBase>()?.baseClass ?: throw MissingIdBaseException(this)
    }
