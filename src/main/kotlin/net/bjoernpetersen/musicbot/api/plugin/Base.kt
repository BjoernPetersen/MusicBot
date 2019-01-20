package net.bjoernpetersen.musicbot.api.plugin

/**
 * Marks a base class or interface of a plugin which may be used to depend on the plugin.
 *
 * Annotated classes or interfaces must extend/implement a plugin interface.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Base

/**
 * Marks a base type which is required even if no plugin depends on it.
 *
 * Note that for active bases, several plugins may be configured.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Base
@MustBeDocumented
annotation class ActiveBase

/**
 * Marks a base type which is used to identify a specific plugin implementing an
 * [active][ActiveBase] base.
 *
 * An ID base may be `SpotifyProviderBase`, identifying whichever Spotify provider implementation
 * is active at runtime, even if the implementation changes between sessions.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Base
@MustBeDocumented
annotation class IdBase
