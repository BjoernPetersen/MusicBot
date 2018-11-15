package com.github.bjoernpetersen.musicbot.spi.plugin

import java.lang.annotation.Inherited
import kotlin.reflect.KClass

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
