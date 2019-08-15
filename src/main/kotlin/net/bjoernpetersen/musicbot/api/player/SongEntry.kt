package net.bjoernpetersen.musicbot.api.player

import net.bjoernpetersen.musicbot.api.auth.User
import net.bjoernpetersen.musicbot.spi.plugin.Suggester

/**
 * An entry representing a song that either has been played or will be in the future.
 *
 * This is a sealed class with exactly two implementations:
 *
 * - [QueueEntry]
 * - [SuggestedSongEntry]
 */
sealed class SongEntry {
    abstract val song: Song

    /**
     * The user that is responsible for this entry's existence.
     */
    abstract val user: User?
}

/**
 * An entry that is or has been in the queue. All queue entries must be created by / associated with
 * a user.
 */
data class QueueEntry(override val song: Song, override val user: User) : SongEntry()

/**
 * An entry that has been suggested by a [Suggester], missing an associated user.
 */
data class SuggestedSongEntry(override val song: Song) : SongEntry() {
    override val user: User? = null
}
