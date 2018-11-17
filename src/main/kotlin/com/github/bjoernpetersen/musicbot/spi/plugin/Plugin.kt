package com.github.bjoernpetersen.musicbot.spi.plugin

import com.github.bjoernpetersen.musicbot.api.config.Config
import com.github.bjoernpetersen.musicbot.spi.config.Named
import com.github.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import java.io.IOException

interface Plugin : Named {
    /**
     * An arbitrary plugin name. Keep it short, but descriptive.
     */
    override val name: String

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
