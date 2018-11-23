package net.bjoernpetersen.musicbot.internal.player

import net.bjoernpetersen.musicbot.api.Song
import net.bjoernpetersen.musicbot.api.player.QueueEntry
import net.bjoernpetersen.musicbot.spi.player.QueueChangeListener
import net.bjoernpetersen.musicbot.spi.player.SongQueue
import java.util.*
import javax.inject.Inject

internal class DefaultQueue @Inject private constructor() : SongQueue {

    private val queue: LinkedList<QueueEntry> = LinkedList()
    private val listeners: MutableSet<QueueChangeListener> = HashSet()

    override val isEmpty: Boolean
        get() = queue.isEmpty()

    override fun insert(entry: QueueEntry) {
        if (!queue.contains(entry)) {
            queue.add(entry)
            notifyListeners { listener -> listener.onAdd(entry) }
        }
    }

    override fun remove(song: Song) {
        val matching = queue.filter { it.song == song }
        matching.forEach {
            queue.remove(it)
            notifyListeners { listener -> listener.onRemove(it) }
        }
    }

    override fun pop(): QueueEntry? {
        return if (queue.isEmpty()) null
        else {
            val entry = queue.pop()
            notifyListeners { listener -> listener.onRemove(entry) }
            entry
        }
    }

    override fun clear() {
        queue.clear()
    }

    private operator fun get(index: Int): Song {
        return queue[index].song
    }

    override fun toList(): List<QueueEntry> {
        return Collections.unmodifiableList(queue)
    }

    /**
     *
     * Moves the specified QueueEntry to the specified index in the queue.
     *
     *   * If the QueueEntry is not in the queue, this method does nothing.  * If the index
     * is greater than the size of the queue, the entry is moved to the end of the queue.
     *
     * @param queueEntry a QueueEntry
     * @param index a 0-based index
     * @throws IllegalArgumentException if the index is smaller than 0
     */
    override fun move(queueEntry: QueueEntry, index: Int) {
        if (index < 0) {
            throw IllegalArgumentException("Index below 0")
        }

        val oldIndex = queue.indexOf(queueEntry)
        if (oldIndex > -1) {
            val newIndex = Math.min(queue.size - 1, index)
            if (oldIndex != newIndex) {
                if (queue.remove(queueEntry)) {
                    queue.add(newIndex, queueEntry)
                    listeners.forEach { l -> l.onMove(queueEntry, oldIndex, newIndex) }
                }
            }
        }
    }

    override fun addListener(listener: QueueChangeListener) {
        listeners.add(Objects.requireNonNull(listener))
    }

    override fun removeListener(listener: QueueChangeListener) {
        listeners.remove(Objects.requireNonNull(listener))
    }

    private fun notifyListeners(notifier: (QueueChangeListener) -> Unit) {
        for (listener in listeners) {
            notifier(listener)
        }
    }
}
