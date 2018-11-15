package com.github.bjoernpetersen.musicbot.spi.player

interface SongPlayedNotifier {
    fun notifyPlayed(songEntry: SongEntry)
}
