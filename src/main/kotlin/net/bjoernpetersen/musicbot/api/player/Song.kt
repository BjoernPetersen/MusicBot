package net.bjoernpetersen.musicbot.api.player

import net.bjoernpetersen.musicbot.api.plugin.NamedPlugin
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.id

data class Song private constructor(
    val id: String,
    val provider: NamedPlugin<Provider>,
    val title: String,
    val description: String,
    val duration: Int? = null,
    val albumArtUrl: String? = null) {

    @JvmOverloads
    constructor(id: String,
        provider: Provider,
        title: String,
        description: String,
        duration: Int? = null,
        albumArtUrl: String? = null) : this(
        id = id,
        provider = NamedPlugin(provider.id.qualifiedName!!,
            provider.subject),
        title = title,
        description = description,
        duration = duration,
        albumArtUrl = albumArtUrl)
}
