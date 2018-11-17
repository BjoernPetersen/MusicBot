package com.github.bjoernpetersen.musicbot.spi.player

import com.github.bjoernpetersen.musicbot.api.player.SongEntry

interface SongPlayedNotifier {
    fun notifyPlayed(songEntry: SongEntry)
}
