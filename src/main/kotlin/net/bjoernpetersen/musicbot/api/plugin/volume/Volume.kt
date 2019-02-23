package net.bjoernpetersen.musicbot.api.plugin.volume

/**
 * Represents volume info as it may be serialized and returned to users.
 *
 * @param volume the volume, a value between 0 and 100 (inclusive).
 * @param isSupported whether getting and setting the volume is actually supported.
 * If this is false, volume will always be 100.
 */
@Suppress("DataClassPrivateConstructor")
data class Volume private constructor(
    val volume: Int,
    val isSupported: Boolean) {

    constructor(volume: Int) : this(volume, true)
    constructor() : this(100, false)
}
