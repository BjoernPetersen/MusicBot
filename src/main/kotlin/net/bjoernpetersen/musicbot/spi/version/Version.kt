package net.bjoernpetersen.musicbot.spi.version

/**
 * Constains version information about the MusicBot instance
 */
interface Version {
    /**
     * Return the VersionInfo
     */
    val versionInfo: VersionInfo
}

/**
 * Contains information about API version, name of the MusicBot instance and implementation information
 */
data class VersionInfo(
    val apiVersion: String,
    val botName: String,
    val implementation: ImplementationInfo
)

/**
 * Implementation information about the MusicBot
 */
data class ImplementationInfo(val projectInfo: String, val name: String, val version: String)
