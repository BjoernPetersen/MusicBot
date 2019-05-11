package net.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.Provides
import net.bjoernpetersen.musicbot.api.config.ConfigManager
import javax.inject.Singleton

class ConfigModule(private val configManager: ConfigManager) : AbstractModule() {
    @Provides
    @Singleton
    fun configManager() = configManager
}
