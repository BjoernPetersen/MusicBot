package net.bjoernpetersen.musicbot.spi.player

import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.player.QueueEntry

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
     * Moves the specified QueueEntry to the specified index in the queue.
     *
     * - If the QueueEntry is not in the queue, this method does nothing.
     * - If the index is greater than the size of the queue,
     * the entry is moved to the end of the queue.
     *
     * @param queueEntry a QueueEntry
     * @param index a 0-based index
     * @throws IllegalArgumentException if the index is smaller than 0
     */
    fun move(queueEntry: QueueEntry, index: Int)
}
