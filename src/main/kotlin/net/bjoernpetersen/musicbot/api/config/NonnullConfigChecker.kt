package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.spi.config.ConfigChecker

/**
 * A config checker that accepts any value except null.
 */
object NonnullConfigChecker : ConfigChecker<Any> {

    override fun invoke(p1: Any?): String? = if (p1 == null) "Required"
    else null
}
