package net.bjoernpetersen.musicbot.api.auth

/**
 * This exception is thrown if a user can't be created because it already exists.
 */
class DuplicateUserException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
