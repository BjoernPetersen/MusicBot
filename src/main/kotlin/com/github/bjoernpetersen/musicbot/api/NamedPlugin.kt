package com.github.bjoernpetersen.musicbot.api

import com.github.bjoernpetersen.musicbot.spi.plugin.Plugin

data class NamedPlugin<out T : Plugin>(val id: String, val name: String) {
    constructor(id: Class<out T>, name: String) : this(id.name, name)
}
