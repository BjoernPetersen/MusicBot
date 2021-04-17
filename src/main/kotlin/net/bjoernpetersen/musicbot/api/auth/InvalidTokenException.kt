package net.bjoernpetersen.musicbot.api.auth

/**
 * This exception is thrown if a token has an invalid structure or its signature can't be verified.
 */
open class InvalidTokenException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * Thrown if the token is invalid because its signature didn't match.
 */
class InvalidSignatureException : InvalidTokenException {
    constructor() : super()
    constructor(cause: Throwable) : super(cause)
}
