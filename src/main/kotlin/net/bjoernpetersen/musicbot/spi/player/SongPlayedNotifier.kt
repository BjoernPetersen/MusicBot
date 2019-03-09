package net.bjoernpetersen.musicbot.spi.player

import net.bjoernpetersen.musicbot.api.player.SongEntry

interface SongPlayedNotifier {
    suspend fun notifyPlayed(songEntry: SongEntry)
}
