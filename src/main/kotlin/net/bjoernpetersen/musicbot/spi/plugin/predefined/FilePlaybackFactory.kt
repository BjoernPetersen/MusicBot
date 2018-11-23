package net.bjoernpetersen.musicbot.spi.plugin.predefined

import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import java.io.File
import java.io.IOException

/**
 * An extension of the {@link PlaybackFactory} interface which accepts input files.
 */
interface FilePlaybackFactory : PlaybackFactory {

    /**
     * Creates a playback object from the given input file.
     *
     * This method can perform blocking IO actions.
     *
     * @param inputFile the input file with audio data
     * @return a Playback object
     * @throws UnsupportedAudioFileException if the format of the input stream is unsupported
     * @throws IOException if any IO error occurs
     */
    @Throws(UnsupportedAudioFileException::class, IOException::class)
    fun createPlayback(inputFile: File): Playback
}

class UnsupportedAudioFileException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
