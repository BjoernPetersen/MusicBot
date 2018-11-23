package net.bjoernpetersen.musicbot.spi.player

import net.bjoernpetersen.musicbot.api.player.QueueEntry

interface QueueChangeListener {
    fun onAdd(entry: QueueEntry)
    fun onRemove(entry: QueueEntry)
    fun onMove(entry: QueueEntry, fromIndex: Int, toIndex: Int)
}
