package net.bjoernpetersen.musicbot.api.auth

/**
 * Thrown if a user is looked up or updated even though he does not exist.
 */
class UserNotFoundException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
