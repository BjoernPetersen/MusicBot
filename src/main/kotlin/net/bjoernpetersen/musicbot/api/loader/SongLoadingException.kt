package net.bjoernpetersen.musicbot.api.loader

/**
 * This exception is thrown if a song could not be loaded.
 */
class SongLoadingException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
