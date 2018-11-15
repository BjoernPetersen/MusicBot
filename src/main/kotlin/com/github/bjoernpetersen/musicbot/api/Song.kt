package com.github.bjoernpetersen.musicbot.api

import com.github.bjoernpetersen.musicbot.spi.plugin.Provider

data class Song(
    val id: String,
    val provider: NamedPlugin<Provider>,
    val title: String,
    val description: String,
    val duration: Int?,
    val albumArtUrl: String?)
