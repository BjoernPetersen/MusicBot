package net.bjoernpetersen.musicbot.api.player

import net.bjoernpetersen.musicbot.api.auth.User

sealed class SongEntry {
    abstract val song: Song
    abstract val user: User?
}

data class QueueEntry(override val song: Song, override val user: User) : SongEntry()
data class SuggestedSongEntry(override val song: Song) : SongEntry() {
    override val user: User? = null
}
