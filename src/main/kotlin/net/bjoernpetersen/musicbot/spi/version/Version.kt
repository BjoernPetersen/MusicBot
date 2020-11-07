package net.bjoernpetersen.musicbot.spi.version

/**
 * Contains information about API version, name of the MusicBot instance and implementation information
 *
 * @param apiVersion The API version
 * @param botName A user-friendly name for the bot instance. May be customized per instance.
 * @param implementation Information about the implementation serving the API.
 *
 */
data class Version(
    val apiVersion: String,
    val botName: String,
    val implementation: ImplementationInfo
)

/**
 * Implementation information about the MusicBot
 *
 * @param projectInfo URL to the project website.
 * @param name The name of the server implementation.
 * @param version The version of the implementation.
 */
data class ImplementationInfo(
    val projectInfo: String,
    val name: String,
    val version: String
)
