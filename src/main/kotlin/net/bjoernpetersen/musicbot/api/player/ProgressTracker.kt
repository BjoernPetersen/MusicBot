package net.bjoernpetersen.musicbot.api.player

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the progress of the current song.
 */
@Singleton
class ProgressTracker @Inject private constructor() {
    private val logger = KotlinLogging.logger {}

    private val mutex = Mutex()

    private var currentStart: Instant? = null
    private var pausedSince: Instant? = null

    /**
     * Gets the progress of the current song.
     *
     * If no song is currently playing, a paused "zero"-progress will be returned.
     */
    suspend fun getCurrentProgress(): SongProgress {
        mutex.withLock {
            logger.debug { "Get progress" }
            val currentStart = currentStart ?: return SongProgress(Duration.ZERO, true)
            val pausedSince = pausedSince
            return if (pausedSince == null)
                SongProgress(Duration.between(currentStart, Instant.now()), false)
            else
                SongProgress(Duration.between(currentStart, pausedSince), true)
        }
    }

    /**
     * Resets the tracker, indicating that there is no song currently playing.
     */
    suspend fun reset() {
        mutex.withLock {
            logger.debug { "Reset" }
            currentStart = null
            pausedSince = null
        }
    }

    /**
     * Indicates that a new song has been started.
     *
     * This will overwrite the old state, as if [reset] had been called before.
     */
    suspend fun startSong() {
        mutex.withLock {
            logger.debug { "Start" }
            currentStart = Instant.now()
            pausedSince = null
        }
    }

    /**
     * Indicates that a pause has been started.
     *
     * If no song is currently playing (indicated by [startSong]), this does nothing.
     *
     * If this method is called again without calling [stopPause] before, this does nothing.
     */
    suspend fun startPause() {
        mutex.withLock {
            logger.debug { "Pause" }
            if (pausedSince == null && currentStart != null) {
                pausedSince = Instant.now()
            }
        }
    }

    /**
     * Stops the current pause, indicating that the current song playback has been resumed.
     *
     * If [startPause] hasn't been called before and/or there is no current song playing, this does
     * nothing.
     */
    suspend fun stopPause() {
        mutex.withLock {
            logger.debug { "Unpause" }
            val currentStart = currentStart
            val pausedSince = pausedSince
            if (currentStart == null || pausedSince == null) {
                return
            }
            val pauseDuration = Duration.between(pausedSince, Instant.now())
            this.currentStart = currentStart.plus(pauseDuration)
            this.pausedSince = null
        }
    }

    /**
     * Manually updates the progress, potentially improving accuracy.
     *
     * If the [progress] is negative, a zero-duration-progress will be used instead.
     *
     * If no song is playing, this method does nothing.
     */
    suspend fun updateProgress(progress: Duration) {
        mutex.withLock {
            if (currentStart == null) return
            val corrected = if (progress.isNegative) {
                logger.warn { "Got negative updated progress. Defaulting to 0." }
                Duration.ZERO
            } else progress
            val pausedSince = pausedSince
            val now = pausedSince ?: Instant.now()
            currentStart = now.minus(corrected)
        }
    }
}

/**
 * Progress of a song.
 *
 * @param duration the duration the song has played
 * @param isPaused whether the song is paused. Important for extrapolation.
 */
data class SongProgress(val duration: Duration, val isPaused: Boolean)
