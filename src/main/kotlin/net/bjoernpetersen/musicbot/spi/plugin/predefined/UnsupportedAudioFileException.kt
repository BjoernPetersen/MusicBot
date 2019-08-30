package net.bjoernpetersen.musicbot.spi.plugin.predefined

/**
 * Thrown by [FilePlaybackFactory] if it can't create a Playback object for a specific file
 * because it doesn't support its format.
 */
class UnsupportedAudioFileException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
