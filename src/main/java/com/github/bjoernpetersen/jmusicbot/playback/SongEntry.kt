package com.github.bjoernpetersen.jmusicbot.playback

import com.github.bjoernpetersen.jmusicbot.Song
import com.github.bjoernpetersen.jmusicbot.user.User

sealed class SongEntry {
    abstract val song: Song
    abstract val user: User?
}

data class QueueEntry(override val song: Song, override val user: User) : SongEntry()
data class SuggestedSongEntry(override val song: Song) : SongEntry() {
    override val user: User? = null
}
