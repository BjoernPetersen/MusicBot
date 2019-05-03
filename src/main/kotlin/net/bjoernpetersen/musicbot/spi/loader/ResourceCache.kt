package net.bjoernpetersen.musicbot.spi.loader

import net.bjoernpetersen.musicbot.api.player.Song

/**
 * Manages resources and actively frees unused resources.
 */
interface ResourceCache {

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
