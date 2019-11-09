package net.bjoernpetersen.musicbot.spi.plugin

import kotlin.reflect.KClass
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.loader.NoResource
import net.bjoernpetersen.musicbot.api.loader.SongLoadingException
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.plugin.ActiveBase
import net.bjoernpetersen.musicbot.spi.loader.Resource

/**
 * A plugin that provides songs from somewhere. That may be an external service, local files, or
 * basically anything you can manage to implement.
 */
@ActiveBase
interface Provider : Plugin, UserFacing {

    /**
     * Searches for songs based on the given search query.
     *
     * It is recommended only to return about 30 songs at max.
     *
     * @param query a search query, trimmed and not empty
     * @param offset the index of the first result to return (may be used for pagination)
     * @return a list of songs
     */
    suspend fun search(query: String, offset: Int = 0): List<Song>

    /**
     * Looks up a song by its ID.
     *
     * @param id the song ID
     * @return the song with the specified ID
     * @throws NoSuchSongException if the ID is invalid
     */
    @Throws(NoSuchSongException::class)
    suspend fun lookup(id: String): Song

    /**
     * Supplies the playback object for the specified song.
     *
     * This method should call a [PlaybackFactory].
     *
     * @param song a song
     * @param resource the resource returned by [loadSong] for the song. Guaranteed to be valid.
     * @return a Playback object
     * @throws Exception if the playback could not be created
     */
    @Throws(Exception::class)
    suspend fun supplyPlayback(song: Song, resource: Resource): Playback

    /**
     * Loads a song, i.e. prepares it before it can be played.
     *
     * This method can potentially download a song before it is being played.
     * The song might never be actually played, so this should never
     * prepare a [Playback] object in any way.
     *
     * The returned resource may be freed without notifying the Provider whenever it is deemed to
     * be unused. As long as the resource is [valid][Resource.isValid], it may be reused multiple
     * times without calling this method again.
     *
     * If you don't allocate any resources, feel free to return [NoResource].
     *
     * This method will not be called on an UI Thread, so it may block.
     *
     * @param song the song to load
     * @return a resource, if any has been allocated
     * @throws SongLoadingException if the song couldn't be loaded
     */
    @Throws(SongLoadingException::class)
    suspend fun loadSong(song: Song): Resource

    /**
     * Looks up a batch of song IDs. If any can't be looked up, they will be dropped.
     *
     * @param ids a list of song IDs
     * @return a list of songs
     */
    suspend fun lookupBatch(ids: List<String>): List<Song> {
        val result = ArrayList<Song>(ids.size)
        for (id in ids) {
            try {
                result.add(lookup(id))
            } catch (e: NoSuchSongException) {
                KotlinLogging.logger { }.warn(e) { "Could not find a song in a batch lookup" }
            }
        }
        return result.toList()
    }
}

/**
 * Thrown if a song could not be found.
 */
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

    /**
     * Specify the ID of the missing song and the most specific base of the failed provider.
     * @param id a song ID
     * @param providerBase the most specific base of the throwing provider
     */
    constructor(id: String, providerBase: Class<out Provider>) : this(id, providerBase.kotlin)

    /**
     * Specify the ID of the missing song.
     * @param id a song ID
     * @param cause the cause for this exception
     */
    constructor(id: String, cause: Throwable) : super("Could not find song with ID $id", cause)

    /**
     * Specify the ID of the missing song and the most specific base of the failed provider.
     * @param id a song ID
     * @param providerBase the most specific base of the throwing provider
     * @param cause the cause for this exception
     */
    constructor(id: String, providerBase: KClass<out Provider>, cause: Throwable) :
        super("Provider ${providerBase.qualifiedName} could not find song with ID $id", cause)

    /**
     * Specify the ID of the missing song and the most specific base of the failed provider.
     * @param id a song ID
     * @param providerBase the most specific base of the throwing provider
     * @param cause the cause for this exception
     */
    constructor(id: String, providerBase: Class<out Provider>, cause: Throwable) :
        this(id, providerBase.kotlin, cause)
}
