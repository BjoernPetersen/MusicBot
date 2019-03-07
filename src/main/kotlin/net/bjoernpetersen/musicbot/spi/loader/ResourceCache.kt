package net.bjoernpetersen.musicbot.spi.loader

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.bjoernpetersen.musicbot.api.async.asFuture
import net.bjoernpetersen.musicbot.api.player.Song
import java.util.concurrent.Future

/**
 * Manages resources and actively frees unused resources.
 */
interface ResourceCache {

    /**
     * Looks up the resource from the cache, otherwise invokes [SongLoader.startLoading].
     *
     * @return a future which eventually contains a resource
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Deprecated for removal in favor of get")
    fun getFuture(song: Song): Future<Resource> = runBlocking {
        async {
            get(song)
        }.asFuture()
    }

    /**
     * Looks up the resource from the cache, otherwise invokes [SongLoader.load].
     *
     * @return a resource
     */
    suspend fun get(song: Song): Resource

    /**
     * Frees all resources and renders this instance unusable.
     */
    suspend fun close()
}
