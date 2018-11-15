package com.github.bjoernpetersen.musicbot.spi.loader

import com.github.bjoernpetersen.musicbot.api.Song
import com.github.bjoernpetersen.musicbot.spi.plugin.Provider
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
    fun startLoading(provider: Provider, song: Song): Future<Boolean>

    @Throws(InterruptedException::class)
    fun close()
}
