package net.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.Scopes
import net.bjoernpetersen.musicbot.internal.auth.DefaultDatabase
import net.bjoernpetersen.musicbot.internal.image.DefaultImageCache
import net.bjoernpetersen.musicbot.internal.loader.DefaultResourceCache
import net.bjoernpetersen.musicbot.internal.loader.DefaultSongLoader
import net.bjoernpetersen.musicbot.internal.player.ActorPlaybackFeedbackChannel
import net.bjoernpetersen.musicbot.internal.player.ActorPlayer
import net.bjoernpetersen.musicbot.internal.player.DefaultQueue
import net.bjoernpetersen.musicbot.spi.auth.UserDatabase
import net.bjoernpetersen.musicbot.spi.image.ImageCache
import net.bjoernpetersen.musicbot.spi.loader.ResourceCache
import net.bjoernpetersen.musicbot.spi.loader.SongLoader
import net.bjoernpetersen.musicbot.spi.player.Player
import net.bjoernpetersen.musicbot.spi.player.SongQueue
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFeedbackChannel
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import javax.inject.Singleton

class DefaultPlayerModule(suggester: Suggester?) : PlayerModule(suggester) {
    @Provides
    @Singleton
    fun provideFeedbackChannel(injector: Injector): PlaybackFeedbackChannel {
        return injector.getInstance(ActorPlaybackFeedbackChannel::class.java)
    }

    override fun configure() {
        super.configure()
        bind(Player::class.java).to(ActorPlayer::class.java).`in`(Scopes.SINGLETON)
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

class DefaultResourceCacheModule : AbstractModule() {
    override fun configure() {
        bind(ResourceCache::class.java).to(DefaultResourceCache::class.java).`in`(Scopes.SINGLETON)
    }
}

class DefaultImageCacheModule : AbstractModule() {
    override fun configure() {
        bind(ImageCache::class.java).to(DefaultImageCache::class.java).`in`(Scopes.SINGLETON)
    }
}
