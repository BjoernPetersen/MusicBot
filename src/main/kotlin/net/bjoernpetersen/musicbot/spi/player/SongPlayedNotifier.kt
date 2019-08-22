package net.bjoernpetersen.musicbot.spi.player

import net.bjoernpetersen.musicbot.api.player.SongEntry

/**
 * Responsible for notifying all concerned parties when a song has been played.
 * This includes, but is not limited to, all activated suggesters.
 */
interface SongPlayedNotifier {
    /**
     * Notifies all objects this implementation deems necessary to notify.
     *
     * @param songEntry the SongEntry which has been played
     */
    suspend fun notifyPlayed(songEntry: SongEntry)
}
