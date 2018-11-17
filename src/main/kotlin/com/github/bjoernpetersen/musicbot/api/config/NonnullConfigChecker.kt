package com.github.bjoernpetersen.musicbot.api.config

import com.github.bjoernpetersen.musicbot.spi.config.ConfigChecker

object NonnullConfigChecker : ConfigChecker<Any> {
    override fun invoke(p1: Any?): String? = if (p1 == null) "Required"
    else null
}
