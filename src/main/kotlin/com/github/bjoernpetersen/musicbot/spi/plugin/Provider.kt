package com.github.bjoernpetersen.musicbot.spi.plugin

import com.github.bjoernpetersen.musicbot.api.Song
import java.io.IOException
import kotlin.reflect.KClass

interface Provider : Plugin, UserFacing {

    /**
     * Searches for songs based on the given search query.
     *
     * It is recommended only to return about 30 songs at max.
     *
     * @param query a search query, trimmed and not empty
     * @return a list of songs
     */
    fun search(query: String): List<Song>

    /**
     * Looks up a song by its ID.
     *
     * @param id the song ID
     * @return the song with the specified ID
     * @throws NoSuchSongException if the ID is invalid
     */
    @Throws(NoSuchSongException::class)
    fun lookup(id: String): Song

    fun getPlaybackSupplier(song: Song): PlaybackSupplier

    /**
     * Loads a song, i.e. prepares it before it can be played.
     *
     * This method can potentially download a song before it is being played. The song might never be
     * actually played, so this should not prepare a {@link Playback} object in any way.
     *
     * This method will not be called on an UI Thread, so it may block.
     *
     * @param song the song to load
     * @return whether the song has been loaded successfully
     */
    fun loadSong(song: Song): Boolean

    /**
     * Looks up a batch of song IDs. If any can't be looked up, they will be dropped.
     *
     * @param ids a list of song IDs
     * @return a list of songs
     */
    fun lookupBatch(ids: List<String>): List<Song> {
        val result = ArrayList<Song>(ids.size)
        for (id in ids) {
            try {
                result.add(lookup(id))
            } catch (e: NoSuchSongException) {
                // TODO log or something
            }
        }
        return result.toList()
    }
}

interface PlaybackSupplier {
    /**
     * Supplies the playback object for the specified song.
     *
     * @param song a song
     * @return a Playback object
     * @throws IOException if the playback could not be created
     */
    @Throws(IOException::class)
    fun supply(song: Song): Playback
}

fun playbackSupplier(supplier: (Song) -> Playback) = object : PlaybackSupplier {
    override fun supply(song: Song): Playback = supplier(song)
}

class NoSuchSongException : Exception {
    /**
     * Specify the ID of the missing song.
     * @param id a song ID
     */
    constructor(id: String) : super("Could not find song with ID $id")

    /**
     * Specify the ID of the missing song and the most specific base of the failed provider.
     * @param id a song ID
     * @param providerBase the most specific base of the throwing provider
     */
    constructor(id: String, providerBase: KClass<out Provider>) :
        super("Provider ${providerBase.qualifiedName} could not find song with ID $id")
}
