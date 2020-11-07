package net.bjoernpetersen.musicbot.spi.util

import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import java.io.File

/**
 * Provides a location for plugins to store files in.
 */
interface FileStorage {

    /**
     * Provides a directory for a plugin to store files in.
     *
     * The directory will only be used for a single plugin.
     *
     * The returned directory is guaranteed to exist and actually be a directory.
     *
     * Multiple calls of this method will return the same directory, but be aware that it will also
     * be cleaned if the [clean] parameter is `true`, which you probably don't want.
     *
     * @param plugin the plugin to provide a directory for
     * @param clean whether the directory should be cleaned/emptied
     */
    fun forPlugin(plugin: Plugin, clean: Boolean = false): File
}
