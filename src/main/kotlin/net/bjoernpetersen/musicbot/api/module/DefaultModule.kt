package net.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Scopes
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import javax.inject.Singleton
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

/**
 * Binds the [ActorPlayer] and its [ActorPlaybackFeedbackChannel] for
 * the [Player] and [PlaybackFeedbackChannel] respectively.
 */
class DefaultPlayerModule(suggester: Suggester?) : PlayerModule(suggester) {
    override fun configure() {
        super.configure()
        bind(PlaybackFeedbackChannel::class.java)
            .to(ActorPlaybackFeedbackChannel::class.java)
            .`in`(Scopes.SINGLETON)
        bind(Player::class.java)
            .to(ActorPlayer::class.java)
            .`in`(Scopes.SINGLETON)
    }
}

/**
 * Binds [DefaultQueue] as [SongQueue].
 */
class DefaultQueueModule : AbstractModule() {
    override fun configure() {
        bind(SongQueue::class.java).to(DefaultQueue::class.java).`in`(Scopes.SINGLETON)
    }
}

/**
 * Binds [DefaultSongLoader] as [SongLoader].
 */
class DefaultSongLoaderModule : AbstractModule() {
    override fun configure() {
        bind(SongLoader::class.java).to(DefaultSongLoader::class.java).`in`(Scopes.SINGLETON)
    }
}

/**
 * Binds a JDBC SQLite connection for [Connection] using the specified [databaseFile] path.
 */
class DefaultDatabaseConnectionModule(private val databaseFile: Path) : AbstractModule() {

    /**
     * Provides the connection.
     */
    @Provides
    @Singleton
    fun provideConnection(): Connection = DriverManager.getConnection("jdbc:sqlite:$databaseFile")
}

/**
 * Binds [DefaultDatabase] as [UserDatabase].
 */
class DefaultUserDatabaseModule constructor() : AbstractModule() {
    @Deprecated("Value is ignored")
    @Suppress("UNUSED_PARAMETER")
    constructor(databaseFile: Path) : this()

    @Deprecated("Value is ignored")
    @Suppress("UNUSED_PARAMETER")
    constructor(databaseUrl: String) : this()

    override fun configure() {
        bind(UserDatabase::class.java).to(DefaultDatabase::class.java)
    }
}

/**
 * Binds [DefaultResourceCache] as [ResourceCache].
 */
class DefaultResourceCacheModule : AbstractModule() {
    override fun configure() {
        bind(ResourceCache::class.java).to(DefaultResourceCache::class.java).`in`(Scopes.SINGLETON)
    }
}

/**
 * Binds [DefaultImageCache] as [ImageCache].
 */
class DefaultImageCacheModule : AbstractModule() {
    override fun configure() {
        bind(ImageCache::class.java).to(DefaultImageCache::class.java).`in`(Scopes.SINGLETON)
    }
}
