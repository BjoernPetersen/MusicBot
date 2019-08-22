package net.bjoernpetersen.musicbot.spi.loader

import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.spi.plugin.Provider

/**
 * Responsible for loading songs by calling [Provider.loadSong].
 */
interface SongLoader {

    /**
     * Loads the specified [song] using the specified [provider].
     *
     * @param provider a provider capable of loading [song]
     * @param song a song to be loaded
     * @return a future eventually yielding the result of [Provider.loadSong]
     */
    suspend fun load(provider: Provider, song: Song): Resource

    /**
     * Closes any resources held by this object. After calling this method, this object is unusable.
     */
    suspend fun close()
}
