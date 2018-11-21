package com.github.bjoernpetersen.musicbot.api.module

import com.github.bjoernpetersen.musicbot.internal.auth.DefaultDatabase
import com.github.bjoernpetersen.musicbot.internal.loader.DefaultSongLoader
import com.github.bjoernpetersen.musicbot.internal.player.DefaultPlayer
import com.github.bjoernpetersen.musicbot.internal.player.DefaultQueue
import com.github.bjoernpetersen.musicbot.spi.auth.UserDatabase
import com.github.bjoernpetersen.musicbot.spi.loader.SongLoader
import com.github.bjoernpetersen.musicbot.spi.player.Player
import com.github.bjoernpetersen.musicbot.spi.player.SongQueue
import com.github.bjoernpetersen.musicbot.spi.plugin.Suggester
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Scopes
import javax.inject.Singleton

class DefaultPlayerModule(suggester: Suggester?) : PlayerModule(suggester) {
    override fun configure() {
        super.configure()
        bind(Player::class.java).to(DefaultPlayer::class.java).`in`(Scopes.SINGLETON)
    }
}

class DefaultQueueModule : AbstractModule() {
    override fun configure() {
        bind(SongQueue::class.java).to(DefaultQueue::class.java).`in`(Scopes.SINGLETON)
    }
}

class DefaultSongLoaderModule : AbstractModule() {
    override fun configure() {
        bind(SongLoader::class.java).to(DefaultSongLoader::class.java).`in`(Scopes.SINGLETON)
    }
}

class DefaultUserDatabaseModule(private val databaseUrl: String) : AbstractModule() {
    @Provides
    @Singleton
    fun provideUserDatabase(): UserDatabase = DefaultDatabase(databaseUrl)
}
