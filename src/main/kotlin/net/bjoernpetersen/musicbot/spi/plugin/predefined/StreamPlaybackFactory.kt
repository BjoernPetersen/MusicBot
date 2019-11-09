package net.bjoernpetersen.musicbot.spi.plugin.predefined

import java.io.IOException
import java.net.URL
import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory

/**
 * An extension of the [PlaybackFactory] interface which accepts URLs.
 */
interface StreamPlaybackFactory : PlaybackFactory {
    /**
     * Creates a playback object from the media file at the given URL.
     *
     * @param streamLocation the URL at which the stream can be found
     * @return a Playback object
     * @throws UnsupportedAudioFileException if the format of the input stream is unsupported
     * @throws IOException if any IO error occurs
     */
    @Throws(UnsupportedAudioFileException::class, IOException::class)
    suspend fun createPlayback(streamLocation: URL): Playback
}
