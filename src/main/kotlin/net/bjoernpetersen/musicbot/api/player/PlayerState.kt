package net.bjoernpetersen.musicbot.api.player

/**
 * Represents the state of the player. All possible implementations can be found in this module.
 */
sealed class PlayerState {

    /**
     * A simple string representation of this state. Can be play, pause, stop or error.
     */
    abstract val name: String
    /**
     * The currently active [SongEntry].
     */
    abstract val entry: SongEntry?

    /**
     * Convenience method to determine whether the current state has a song (entry != null).
     */
    fun hasSong() = entry != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerState

        if (entry != other.entry) return false

        return true
    }

    override fun hashCode(): Int {
        return entry?.hashCode() ?: 0
    }
}

/**
 * The player is currently active and is playing the song from [entry].
 */
data class PlayState(override val entry: SongEntry) : PlayerState() {

    override val name = "play"

    /**
     * Creates a [PauseState] with the same entry.
     */
    fun pause() = PauseState(entry)
}

/**
 * The player is currently inactive, but has an active [entry].
 */
data class PauseState(override val entry: SongEntry) : PlayerState() {

    override val name = "pause"

    /**
     * Creates a [PlayState] with the same entry.
     */
    fun play() = PlayState(entry)
}

/**
 * The player is currently inactive and does not have a song to resume.
 *
 * The player may automatically resume playback as soon as a next song is available.
 */
object StopState : PlayerState() {

    override val name = "stop"
    override val entry: SongEntry? = null
    override fun toString(): String = "StopState"
}

/**
 * The player is currently inactive because of an error.
 *
 * The only way to leave this state is by explicit user intervention. The player should not try to
 * play a next song automatically.
 */
object ErrorState : PlayerState() {

    override val name = "error"
    override val entry: SongEntry? = null
    override fun toString(): String = "ErrorState"
}
