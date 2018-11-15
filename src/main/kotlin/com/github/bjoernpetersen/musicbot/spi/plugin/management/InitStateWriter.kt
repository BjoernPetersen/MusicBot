package com.github.bjoernpetersen.musicbot.spi.plugin.management

import com.github.bjoernpetersen.musicbot.spi.plugin.Plugin

interface InitStateWriter {
    fun begin(plugin: Plugin)
    fun state(state: String)
    fun warning(warning: String)
    fun close() {}
}
