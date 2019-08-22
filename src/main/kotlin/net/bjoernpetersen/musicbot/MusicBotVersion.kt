package net.bjoernpetersen.musicbot

import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.Version
import java.io.IOException
import java.util.Properties

/**
 * Utility object to get the version of the current bot.
 *
 * This object only works correctly if a `version.properties` file is present (which it is in Gradle
 * builds).
 */
object MusicBotVersion {
    /**
     * Gets the version of this MusicBot.
     *
     * @return a version
     */
    @Suppress("ThrowsCount")
    fun get(): Version {
        try {
            val properties = Properties()
            Version::class.java.getResourceAsStream("version.properties")
                .use { versionStream -> properties.load(versionStream) }
            val version = properties.getProperty("version")
                ?: throw IllegalStateException("Version is missing")
            return Version.valueOf(Version.valueOf(version).normalVersion)
        } catch (e: IOException) {
            throw IllegalStateException("Could not read version resource", e)
        } catch (e: ParseException) {
            throw IllegalStateException("Could not read version resource", e)
        }
    }
}
