package com.github.bjoernpetersen.jmusicbot.playback

sealed class PlayerState {
  abstract val entry: SongEntry?
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

data class PlayState(override val entry: SongEntry) : PlayerState() {
  fun pause() = PauseState(entry)
}

data class PauseState(override val entry: SongEntry) : PlayerState() {
  fun play() = PlayState(entry)
}

class StopState : PlayerState() {
  override val entry: SongEntry? = null
  override fun toString(): String = "StopState"
}

class ErrorState : PlayerState() {
  override val entry: SongEntry? = null
  override fun toString(): String = "ErrorState"
}
