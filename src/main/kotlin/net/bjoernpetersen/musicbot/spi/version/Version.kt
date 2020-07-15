package net.bjoernpetersen.musicbot.spi.version

/**
 * Contains version information about the MusicBot instance
 */
data class Version (
    /**
     * Version information about the bot
     */
    val versionInfo: VersionInfo
)

/**
 * Contains information about API version, name of the MusicBot instance and implementation information
 */
data class VersionInfo(
    /**
     * The API version
     */
    val apiVersion: String,
    /**
     * A user-friendly name for the bot instance. May be customized per instance.
     */
    val botName: String,
    /**
     * Information about the implementation serving the API.
     */
    val implementation: ImplementationInfo
)

/**
 * Implementation information about the MusicBot
 */
data class ImplementationInfo(
    /**
     * The name of the server implementation.
     */
    val projectInfo: String,
    /**
     * The version of the implementation.
     */
    val name: String,
    /**
     * URL to the project website.
     */
    val version: String)
