package net.bjoernpetersen.musicbot.spi.player

import net.bjoernpetersen.musicbot.api.player.SongEntry

interface PlayerHistory {
    fun getHistory(limit: Int = 20): List<SongEntry>
}
