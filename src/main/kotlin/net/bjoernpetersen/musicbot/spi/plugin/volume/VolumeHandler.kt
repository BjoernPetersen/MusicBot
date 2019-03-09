package net.bjoernpetersen.musicbot.spi.plugin.volume

import net.bjoernpetersen.musicbot.api.plugin.ActiveBase
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin

/**
 * A kind of [GenericPlugin] capable of changing volume.
 *
 * Should still be registered as a GenericPlugin in the services manifest.
 */
@IdBase("Volume handler")
@ActiveBase
interface VolumeHandler : GenericPlugin {

    /**
     * Gets the current volume, between 0 and 100, inclusively.
     *
     * This may represent the system volume, but it could also represent the volume of anything
     * else, like the volume of a Chromecast or a remote Spotify client.
     */
    suspend fun getVolume(): Int

    /**
     * Set the current volume, between 0 and 100, inclusively.
     *
     * This may represent the system volume, but it could also represent the volume of anything
     * else, like the volume of a Chromecast or a remote Spotify client.
     *
     * @throws IllegalArgumentException if [value] is not between 0 and 100
     */
    suspend fun setVolume(value: Int)


}
