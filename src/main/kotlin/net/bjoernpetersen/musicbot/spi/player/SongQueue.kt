package net.bjoernpetersen.musicbot.spi.player

import net.bjoernpetersen.musicbot.api.player.QueueEntry
import net.bjoernpetersen.musicbot.api.player.Song

/**
 * A queue containing instances of [QueueEntry].
 *
 * The queue may never contain duplicates.
 */
interface SongQueue {

    val isEmpty: Boolean
    fun pop(): QueueEntry?
    fun insert(entry: QueueEntry)
    fun remove(song: Song)
    fun clear()
    fun toList(): List<QueueEntry>

    fun addListener(listener: QueueChangeListener)
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
