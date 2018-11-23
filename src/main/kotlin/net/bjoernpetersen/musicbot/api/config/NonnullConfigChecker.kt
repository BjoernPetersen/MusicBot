package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.spi.config.ConfigChecker

object NonnullConfigChecker : ConfigChecker<Any> {
    override fun invoke(p1: Any?): String? = if (p1 == null) "Required"
    else null
}
