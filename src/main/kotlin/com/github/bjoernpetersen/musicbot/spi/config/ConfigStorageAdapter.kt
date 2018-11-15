package com.github.bjoernpetersen.musicbot.spi.config

import com.github.bjoernpetersen.musicbot.api.config.ConfigScope

/**
 * An adapter for loading and storing config entries.
 */
interface ConfigStorageAdapter {

    fun load(scope: ConfigScope): Map<String, String>

    /**
     * Stores the config entries in the specified map.
     *
     * This method is likely to be called often, so it should be fast and should not need user
     * interaction.
     *
     * @param scope the scope of the config
     * @param config a map of config entries with values
     */
    fun store(scope: ConfigScope, config: Map<String, String>)
}


