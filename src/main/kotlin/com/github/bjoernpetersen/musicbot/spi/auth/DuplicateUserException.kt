package com.github.bjoernpetersen.musicbot.spi.auth

class DuplicateUserException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
