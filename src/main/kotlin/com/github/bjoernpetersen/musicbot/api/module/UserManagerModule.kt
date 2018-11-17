package com.github.bjoernpetersen.musicbot.api.module

import com.github.bjoernpetersen.musicbot.api.auth.UserManager
import com.github.bjoernpetersen.musicbot.api.config.ConfigManager
import com.github.bjoernpetersen.musicbot.spi.auth.UserDatabase
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton

class UserManagerModule : AbstractModule() {
    @Provides
    @Singleton
    fun provideUserManager(database: UserDatabase, configManager: ConfigManager) =
        UserManager(database, configManager)
}
