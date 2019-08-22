package net.bjoernpetersen.musicbot.spi.config

import net.bjoernpetersen.musicbot.api.config.ConfigScope

/**
 * An adapter for loading and storing config entries.
 */
interface ConfigStorageAdapter {

    /**
     * Loads the config entries for the specified scope.
     *
     * @param scope the scope for which to load the entries
     * @return a mapping from config keys to values
     */
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
