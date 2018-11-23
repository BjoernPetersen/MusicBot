package com.github.bjoernpetersen.musicbot.spi.plugin

import com.github.bjoernpetersen.musicbot.api.Song
import com.github.bjoernpetersen.musicbot.api.player.SongEntry

interface Suggester : Plugin, UserFacing, Active {

    /**
     * Suggest the next song to play.
     *
     * @return a song to play
     * @throws BrokenSuggesterException if the suggester can't suggest anything
     */
    @Throws(BrokenSuggesterException::class)
    fun suggestNext(): Song

    /**
     * Gets a list of next suggestions which will be returned by calling [suggestNext].
     *
     * The returned list must contain at least one element.
     *
     * @param maxLength the maximum length of the returned list
     * @return a list of next suggestions
     * @throws BrokenSuggesterException if the suggester can't suggest anything
     */
    @Throws(BrokenSuggesterException::class)
    fun getNextSuggestions(maxLength: Int): List<Song>

    /**
     * Notifies this Suggester that the specified song entry has been played.
     *
     * It is recommended that the song will be removed from the next suggestions when this method
     * is called.
     *
     * It is guaranteed that the song comes from a provider this suggester depends on.
     *
     * The default implementation calls [removeSuggestion].
     *
     * @param songEntry a SongEntry
     */
    fun notifyPlayed(songEntry: SongEntry) {
        removeSuggestion(songEntry.song)
    }

    /**
     *
     * Removes the specified song from the [suggestions][.getNextSuggestions].
     *
     * @param song a song
     */
    fun removeSuggestion(song: Song)

    /**
     * Indicates a user disliking the specified song.
     *
     * In contrast to a [removeSuggestion] call, this method indicates general dislike by the user
     * and that the song should be avoided in the future.
     *
     * The default implementation calls [removeSuggestion].
     *
     * @param song a song
     */
    fun dislike(song: Song) {
        removeSuggestion(song)
    }
}

/**
 * Indicates that a Suggester cannot make a suggestion request right now.
 *
 * This Exception does **not** imply that the suggester will be broken in the future.
 */
class BrokenSuggesterException : Exception {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
