package net.bjoernpetersen.musicbot.api.loader

import net.bjoernpetersen.musicbot.spi.loader.Resource

/**
 * A resource which actually isn't one. It is always valid and all methods are basically no-ops.
 */
object NoResource : Resource {

    override suspend fun free() = Unit
    override val isValid
        get() = true
}
