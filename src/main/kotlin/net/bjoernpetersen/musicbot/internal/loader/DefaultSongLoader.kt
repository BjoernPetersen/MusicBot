package net.bjoernpetersen.musicbot.internal.loader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.loader.SongLoader
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultSongLoader @Inject private constructor() : SongLoader, CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override suspend fun load(provider: Provider, song: Song): Resource {
        return coroutineScope {
            withContext(coroutineContext) {
                provider.loadSong(song)
            }
        }
    }

    override suspend fun close() {
        job.cancelAndJoin()
    }
}
