package net.bjoernpetersen.musicbot.api.plugin.volume

import com.google.inject.ConfigurationException
import com.google.inject.Injector
import net.bjoernpetersen.musicbot.spi.plugin.volume.VolumeHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Allows to get and set volume. Uses the configured [VolumeHandler] instance for that.
 * If no instance has been configured, [getVolume] will indicate that (see [Volume]) and [setVolume]
 * is a no-op.
 */
@Singleton
class VolumeManager @Inject private constructor(injector: Injector) {

    private val handler: VolumeHandler? by lazy {
        try {
            injector.getInstance(VolumeHandler::class.java)
        } catch (e: ConfigurationException) {
            null
        }
    }

    /**
     * Gets the current volume.
     *
     * If no [VolumeHandler] has been configured, this returns an "unsupported" [Volume] object,
     * but will **never throw an exception**.
     */
    fun getVolume(): Volume = handler?.volume?.let(::Volume) ?: Volume()

    /**
     * Sets the volume.
     *
     * If no [VolumeHandler] has been configured, this method does nothing.
     */
    fun setVolume(volume: Int) {
        handler?.let { it.volume = volume }
    }
}
