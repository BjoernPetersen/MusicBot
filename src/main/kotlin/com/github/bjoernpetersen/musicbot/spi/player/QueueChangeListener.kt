package com.github.bjoernpetersen.musicbot.spi.player

interface QueueChangeListener {
    fun onAdd(entry: QueueEntry)
    fun onRemove(entry: QueueEntry)
    fun onMove(entry: QueueEntry, fromIndex: Int, toIndex: Int)
}
