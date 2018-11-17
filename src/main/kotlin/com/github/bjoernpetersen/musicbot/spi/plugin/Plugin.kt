package com.github.bjoernpetersen.musicbot.spi.plugin

import com.github.bjoernpetersen.musicbot.api.config.Config
import com.github.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import java.io.IOException
import javax.inject.Inject

/**
 * Base interface for plugins. This interface isn't meant to be directly implemented, but to be
 * extended by a specialized interface first.
 *
 * ## Dependencies
 *
 * A plugin can request dependencies (typically other plugins) by marking fields with the
 * [Inject] annotation. Please note that dependency injection in constructors is not possible.
 *
 * ## Lifecycle
 *
 * A plugin goes through several different lifecycle phases due to the way it is integrated in the
 * MusicBot.
 *
 * ### Creation
 *
 * The plugin is loaded from an external Jar-File and instantiated using a no-args constructor.
 * In this phase, no actual work should be done by the plugin and no resources should be allocated.
 *
 * ### Dependency injection
 *
 * In a second, **separate** step, the dependencies requested by the plugin are resolved
 * and injected. There is no notification for the plugin about this.
 *
 * ### Configuration
 *
 * The plugin is asked to provide its configuration entries by [createConfigEntries] and
 * [createSecretEntries]. The returned entries are shown to the user and their value may change
 * any number of times during this phase.
 *
 * ### Initialization
 *
 * The [initialize] method is called after configuration is done and the plugin may allocate
 * additional resources for the time it is running.
 *
 * ### Destruction
 *
 * The [close] method is called and all resources should be released.
 * The plugin instance will never be used again at this point.
 *
 */
interface Plugin {

    /**
     * An arbitrary plugin name. Keep it short, but descriptive.
     */
    val name: String

    fun createConfigEntries(config: Config): List<Config.Entry<*>>
    fun createSecretEntries(secrets: Config): List<Config.Entry<*>>
    fun createStateEntries(state: Config)

    @Throws(InitializationException::class)
    fun initialize(initStateWriter: InitStateWriter)

    @Throws(IOException::class)
    fun close()
}

interface UserFacing {
    /**
     * The subject of the content provided by this plugin.
     *
     * This will be shown to end users, together with the type of plugin, this should give users
     * an idea of they will be getting.
     *
     * Some good examples:
     * - For providers: "Spotify" or "YouTube"
     * - For suggesters: "Random MP3s", a playlist name, "Based on last played song"
     */
    val subject: String
}

/**
 * An exception during plugin initialization.
 */
class InitializationException : Exception {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
