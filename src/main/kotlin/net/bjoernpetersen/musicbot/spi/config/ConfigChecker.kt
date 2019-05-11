package net.bjoernpetersen.musicbot.spi.config

import net.bjoernpetersen.musicbot.api.config.NonnullConfigChecker

/**
 * Checks whether the given value is valid.
 *
 * Note that, if you set a default value for the config entry, the input value for the checker will
 * never be null.
 *
 * @see NonnullConfigChecker
 *
 * @return a warning message, or null
 */
typealias ConfigChecker<T> = (configValue: T?) -> String?
