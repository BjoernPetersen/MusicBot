package net.bjoernpetersen.musicbot.internal.player

import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.player.QueueEntry
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.spi.player.QueueChangeListener
import net.bjoernpetersen.musicbot.spi.player.SongQueue
import java.util.Collections
import java.util.HashSet
import java.util.LinkedList
import java.util.Objects
import javax.inject.Inject

internal class DefaultQueue @Inject private constructor() : SongQueue {
    private val logger = KotlinLogging.logger {}

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

    override fun move(queueEntry: QueueEntry, index: Int) {
        if (index < 0) {
            throw IllegalArgumentException("Index below 0")
        }

        val oldIndex = queue.indexOfFirst {
            it.song.id == queueEntry.song.id && it.user.name == queueEntry.user.name
        }
        if (oldIndex > -1) {
            val newIndex = Math.min(queue.size - 1, index)
            logger.debug { "Moving ${queueEntry.song.title} from $oldIndex to $newIndex" }
            if (oldIndex != newIndex) {
                if (queue.remove(queueEntry)) {
                    queue.add(newIndex, queueEntry)
                    listeners.forEach { l -> l.onMove(queueEntry, oldIndex, newIndex) }
                }
            }
        } else logger.debug { "Tried to move song that's not in the queue" }
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
