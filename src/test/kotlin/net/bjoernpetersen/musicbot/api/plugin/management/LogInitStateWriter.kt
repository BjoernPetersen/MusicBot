package net.bjoernpetersen.musicbot.api.plugin.management

import mu.KotlinLogging
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter

class LogInitStateWriter : InitStateWriter {
    private val logger = KotlinLogging.logger { }
    override fun state(state: String) {
        logger.debug { state }
    }

    override fun warning(warning: String) {
        logger.warn { warning }
    }
}
