package com.github.bjoernpetersen.musicbot.api.module

import com.github.bjoernpetersen.musicbot.internal.auth.DefaultDatabase
import com.github.bjoernpetersen.musicbot.internal.loader.DefaultSongLoader
import com.github.bjoernpetersen.musicbot.internal.player.DefaultPlayer
import com.github.bjoernpetersen.musicbot.internal.player.DefaultQueue
import com.github.bjoernpetersen.musicbot.spi.auth.UserDatabase
import com.github.bjoernpetersen.musicbot.spi.loader.SongLoader
import com.github.bjoernpetersen.musicbot.spi.player.Player
import com.github.bjoernpetersen.musicbot.spi.player.SongPlayedNotifier
import com.github.bjoernpetersen.musicbot.spi.player.SongQueue
import com.github.bjoernpetersen.musicbot.spi.plugin.Suggester
import com.github.bjoernpetersen.musicbot.spi.plugin.management.PluginFinder
import com.google.inject.AbstractModule
import com.google.inject.Provides
import javax.inject.Singleton

class DefaultPlayerModule(
    private val suggester: Suggester?,
    private val pluginFinder: PluginFinder) : AbstractModule() {

    @Provides
    @Singleton
    fun providePlayer(
        queue: SongQueue,
        songLoader: SongLoader,
        songPlayedNotifier: SongPlayedNotifier): Player =
        DefaultPlayer(queue, songLoader, pluginFinder, songPlayedNotifier, suggester)
}

class DefaultQueueModule : AbstractModule() {
    @Provides
    @Singleton
    fun provideQueue(): SongQueue = DefaultQueue()
}

class DefaultSongLoaderModule : AbstractModule() {
    @Provides
    @Singleton
    fun provideSongLoader(): SongLoader = DefaultSongLoader()
}

class DefaultUserDatabaseModule(private val databaseUrl: String) : AbstractModule() {
    @Provides
    @Singleton
    fun provideUserDatabase(): UserDatabase = DefaultDatabase(databaseUrl)
}
