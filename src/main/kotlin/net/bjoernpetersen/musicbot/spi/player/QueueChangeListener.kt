package net.bjoernpetersen.musicbot.spi.player

import net.bjoernpetersen.musicbot.api.player.QueueEntry

/**
 * A listener to track changes on the [queue][SongQueue].
 */
interface QueueChangeListener {
    /**
     * Called when an entry is added to the queue.
     *
     * @param entry the added entry
     */
    fun onAdd(entry: QueueEntry)

    /**
     * Called when an entry is removed from the queue.
     *
     * @param entry the removed entry
     */
    fun onRemove(entry: QueueEntry)

    /**
     * Called when an entry has changed position in the queue.
     *
     * @param entry the moved entry
     * @param fromIndex the old index in the queue
     * @param toIndex the index of the entry in the new queue
     */
    fun onMove(entry: QueueEntry, fromIndex: Int, toIndex: Int)
}
