package net.bjoernpetersen.musicbot.spi.plugin

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Base

/**
 * Marks a base type which is required even if no plugin depends on it.
 *
 * Note that for active bases, several plugins may be configured.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Base
annotation class ActiveBase


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Base
annotation class IdBase
