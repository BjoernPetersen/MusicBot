package net.bjoernpetersen.musicbot.api

import net.bjoernpetersen.musicbot.spi.plugin.Provider

data class Song @JvmOverloads constructor(
    val id: String,
    val provider: NamedPlugin<Provider>,
    val title: String,
    val description: String,
    val duration: Int? = null,
    val albumArtUrl: String? = null)
