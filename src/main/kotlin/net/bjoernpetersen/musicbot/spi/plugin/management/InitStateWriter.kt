package net.bjoernpetersen.musicbot.spi.plugin.management

import net.bjoernpetersen.musicbot.spi.plugin.Plugin

interface InitStateWriter {
    fun begin(plugin: Plugin)
    fun state(state: String)
    fun warning(warning: String)
    fun close() {}
}
