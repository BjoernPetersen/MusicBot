package com.github.bjoernpetersen.musicbot.spi.config

/**
 * Checks whether the given value is valid.
 * The default value will never be passed to the checker, even if no other value has been set.
 *
 * @return a warning message, or null
 */
typealias ConfigChecker<T> = (T?) -> String?


