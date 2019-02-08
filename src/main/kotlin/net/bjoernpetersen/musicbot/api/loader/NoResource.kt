package net.bjoernpetersen.musicbot.api.loader

import net.bjoernpetersen.musicbot.spi.loader.Resource

/**
 * A resource which actually isn't one. It is always valid and all methods are basically no-ops.
 */
object NoResource : Resource {

    override fun free() = Unit
    override fun isValid(): Boolean = true
}
