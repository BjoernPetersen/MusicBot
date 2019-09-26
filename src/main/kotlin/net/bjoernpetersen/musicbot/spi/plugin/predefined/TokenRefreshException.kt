package net.bjoernpetersen.musicbot.spi.plugin.predefined

/**
 * Exception that is thrown if a token could not be refreshed.
 */
class TokenRefreshException : Exception {
    /**
     * Exception without further information.
     */
    constructor() : super()

    /**
     * Exception with a message.
     * @param message a message
     */
    constructor(message: String) : super(message)

    /**
     * Exception with a message and a cause.
     * @param message a message
     * @param cause an exception that caused the error
     */
    constructor(message: String, cause: Throwable) : super(message, cause)

    /**
     * Exception with a cause
     * @param cause an exception that caused the error
     */
    constructor(cause: Throwable) : super(cause)
}
