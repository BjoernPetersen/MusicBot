package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.spi.config.ConfigStorageAdapter

/**
 * Manages all config instances and makes them accessible by scope.
 *
 * @param plainStorageAdapter a storage adapter for plaintext entries
 * @param secretStorageAdapter a storage adapter for secret entries
 * @param stateStorageAdapter a storage adapter for state entries which should be easy to delete to
 * reset all state without losing configuration
 */
class ConfigManager(
    private val plainStorageAdapter: ConfigStorageAdapter,
    private val secretStorageAdapter: ConfigStorageAdapter,
    private val stateStorageAdapter: ConfigStorageAdapter
) {

    private val configs: MutableMap<ConfigScope, Configs> = HashMap()

    /**
     * @param scope the scope for which to load the configs
     * @return all configs for that scope
     */
    operator fun get(scope: ConfigScope): Configs = configs.computeIfAbsent(scope) {
        Configs(
            plain = Config(plainStorageAdapter, it),
            secrets = Config(secretStorageAdapter, it),
            state = Config(stateStorageAdapter, it)
        )
    }
}

/**
 * Data class holding all configs for a specific scope.
 *
 * @param plain the plaintext config
 * @param secrets the secret config
 * @param state the state config
 */
data class Configs(
    val plain: Config,
    val secrets: Config,
    val state: Config
)
