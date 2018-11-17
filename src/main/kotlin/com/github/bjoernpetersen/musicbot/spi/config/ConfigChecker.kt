package com.github.bjoernpetersen.musicbot.spi.config

/**
 * Checks whether the given value is valid.
 *
 * Note that, if you set a default value for the config entry, the input value for the checker will
 * never be null (or blank, if it's a String).
 *
 * @return a warning message, or null
 */
typealias ConfigChecker<T> = (T?) -> String?


