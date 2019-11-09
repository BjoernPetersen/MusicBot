package net.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.Provides
import javax.inject.Singleton
import net.bjoernpetersen.musicbot.api.config.ConfigManager

/**
 * Guice module which binds an instance of [ConfigManager].
 *
 * @param configManager the instance to bind for the [ConfigManager] key.
 */
class ConfigModule(private val configManager: ConfigManager) : AbstractModule() {
    /**
     * Supplies a [ConfigManager].
     */
    @Provides
    @Singleton
    fun configManager() = configManager
}
