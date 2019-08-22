package net.bjoernpetersen.musicbot.api.auth

/**
 * This exception is thrown if a token has an invalid structure or its signature can't be verified.
 */
class InvalidTokenException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
