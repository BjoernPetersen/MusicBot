package net.bjoernpetersen.musicbot.spi.player

import net.bjoernpetersen.musicbot.api.player.QueueEntry
import net.bjoernpetersen.musicbot.api.player.Song

/**
 * A queue containing instances of [QueueEntry].
 *
 * The queue may never contain duplicates (multiple entries with the same song).
 */
interface SongQueue {

    /**
     * Whether the queue is currently empty.
     */
    val isEmpty: Boolean

    /**
     * @return the next QueueEntry which should be played, or null if the queue is empty.
     */
    fun pop(): QueueEntry?

    /**
     * Inserts a new entry into the queue
     * @param entry the new entry
     */
    fun insert(entry: QueueEntry)

    /**
     * Removes a song from the queue.
     * @param song the song remove
     */
    fun remove(song: Song)

    /**
     * Clear the queue.
     */
    fun clear()

    /**
     * Create a list containing all entries in this queue, in the order in which they would be
     * returned by [pop].
     *
     * The returned list is not backed by this queue, i.e. it's not updated when this queue changes.
     */
    fun toList(): List<QueueEntry>

    /**
     * Add a listener which will be notified of any changes in the queue.
     */
    fun addListener(listener: QueueChangeListener)

    /**
     * Remove a listener which was previously added by calling [addListener].
     */
    fun removeListener(listener: QueueChangeListener)

    /**
     * Moves the QueueEntry with the specified Song to the specified index in the queue.
     *
     * - If no such QueueEntry is in the queue, this method does nothing.
     * - If the index is greater than the size of the queue,
     * the entry is moved to the end of the queue.
     *
     * @param song a Song
     * @param index a 0-based index
     * @throws IllegalArgumentException if the index is smaller than 0
     */
    fun move(song: Song, index: Int)
}
