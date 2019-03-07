package net.bjoernpetersen.musicbot.spi.loader

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.bjoernpetersen.musicbot.api.async.asFuture
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import java.util.concurrent.Future

/**
 * Responsible for loading songs by calling [Provider.loadSong].
 */
interface SongLoader {

    /**
     * Asynchronously loads the specified [song] using the specified [provider].
     *
     * @param provider a provider capable of loading [song]
     * @param song a song to be loaded
     * @return a future eventually yielding the result of [Provider.loadSong]
     */
    @Deprecated("Use load instead", replaceWith = ReplaceWith("load"))
    fun startLoading(provider: Provider, song: Song): Future<Resource> = runBlocking {
        async {
            load(provider, song)
        }.asFuture()
    }

    /**
     * Loads the specified [song] using the specified [provider].
     *
     * @param provider a provider capable of loading [song]
     * @param song a song to be loaded
     * @return a future eventually yielding the result of [Provider.loadSong]
     */
    suspend fun load(provider: Provider, song: Song): Resource

    suspend fun close()
}
