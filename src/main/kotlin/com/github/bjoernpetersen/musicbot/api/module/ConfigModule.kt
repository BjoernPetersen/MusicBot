package com.github.bjoernpetersen.musicbot.api.module

import com.github.bjoernpetersen.musicbot.api.config.ConfigManager
import com.google.inject.AbstractModule
import com.google.inject.Provides
import javax.inject.Singleton

class ConfigModule(private val configManager: ConfigManager) : AbstractModule() {
    @Provides
    @Singleton
    fun configManager() = configManager
}
