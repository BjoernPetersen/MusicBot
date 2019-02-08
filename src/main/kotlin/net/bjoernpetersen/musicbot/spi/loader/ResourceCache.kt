package net.bjoernpetersen.musicbot.spi.loader

import net.bjoernpetersen.musicbot.api.player.Song
import java.io.IOException
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
    operator fun get(song: Song): Future<Resource>

    /**
     * Frees all resources and renders this instance unusable.
     */
    @Throws(IOException::class)
    fun close()
}
