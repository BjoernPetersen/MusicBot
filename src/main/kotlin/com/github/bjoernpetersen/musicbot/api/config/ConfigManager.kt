package com.github.bjoernpetersen.musicbot.api.config

import com.github.bjoernpetersen.musicbot.spi.config.ConfigStorageAdapter

class ConfigManager(
    private val plainStorageAdapter: ConfigStorageAdapter,
    private val secretStorageAdapter: ConfigStorageAdapter,
    private val stateStorageAdapter: ConfigStorageAdapter) {

    private val configs: MutableMap<ConfigScope, Configs> = HashMap()

    operator fun get(scope: ConfigScope): Configs = configs.computeIfAbsent(scope) {
        Configs(
            plain = Config(plainStorageAdapter, it),
            secrets = Config(secretStorageAdapter, it),
            state = Config(stateStorageAdapter, it)
        )
    }
}

data class Configs(
    val plain: Config,
    val secrets: Config,
    val state: Config)
