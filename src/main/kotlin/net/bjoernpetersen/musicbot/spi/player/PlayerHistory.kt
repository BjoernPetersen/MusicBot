package net.bjoernpetersen.musicbot.spi.player

import net.bjoernpetersen.musicbot.api.player.SongEntry

/**
 * Keeps track of the history of songs played by the [Player].
 *
 * The history is limited to a maximum size, it's not kept indefinitely.
 */
interface PlayerHistory {
    /**
     * Gets the current player history.
     *
     * The returned list is ordered from least to most recently played. It is immutable and will not
     * be updated.
     *
     * @param limit the maximum limit of the result
     * @return a list of recently played songs
     * @throws IllegalArgumentException if the limit is negative
     */
    fun getHistory(limit: Int = 20): List<SongEntry>
}
