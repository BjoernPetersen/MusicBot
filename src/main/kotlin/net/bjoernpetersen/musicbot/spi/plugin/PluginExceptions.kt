package net.bjoernpetersen.musicbot.spi.plugin

import net.bjoernpetersen.musicbot.api.plugin.InitializationException as NewInitException

/**
 * An exception during plugin initialization.
 */
@Deprecated(
    "Use version from api package",
    ReplaceWith("net.bjoernpetersen.musicbot.api.plugin.InitializationException"),
    level = DeprecationLevel.ERROR
)
open class InitializationException : NewInitException {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * An exception thrown by Plugins if they are misconfigured.
 */
@Deprecated(
    "Use version from api package",
    ReplaceWith("net.bjoernpetersen.musicbot.api.plugin.ConfigurationException"),
    level = DeprecationLevel.ERROR
)
class ConfigurationException : NewInitException {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * Thrown if a plugin's declaration is invalid.
 */
@Deprecated(
    "Use version from api package",
    ReplaceWith("net.bjoernpetersen.musicbot.api.plugin.DeclarationException"),
    level = DeprecationLevel.ERROR
)
class DeclarationException : RuntimeException {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
