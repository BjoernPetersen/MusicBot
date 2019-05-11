package net.bjoernpetersen.musicbot.api.player

import net.bjoernpetersen.musicbot.api.plugin.NamedPlugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.id

data class Song internal constructor(
    val id: String,
    val provider: NamedPlugin<Provider>,
    val title: String,
    val description: String,
    val duration: Int? = null,
    val albumArtUrl: String? = null
) {

    @JvmOverloads
    constructor(
        id: String,
        provider: Provider,
        title: String,
        description: String,
        duration: Int? = null,
        albumArtUrl: String? = null
    ) : this(
        id = id,
        provider = NamedPlugin(
            provider.id.qualifiedName!!,
            provider.subject),
        title = title,
        description = description,
        duration = duration,
        albumArtUrl = albumArtUrl)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Song) return false

        if (id != other.id) return false
        if (provider != other.provider) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + provider.hashCode()
        return result
    }
}
