package net.bjoernpetersen.musicbot.api.player

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressTracker @Inject private constructor() {

    private val mutex = Mutex()

    private var currentStart: Instant? = null
    private var pausedSince: Instant? = null

    suspend fun getCurrentProgress(): Duration {
        mutex.withLock {
            val currentStart = currentStart ?: return Duration.ZERO
            val pausedSince = pausedSince
            return if (pausedSince == null) Duration.between(currentStart, Instant.now())
            else Duration.between(currentStart, pausedSince)
        }
    }

    suspend fun reset() {
        mutex.withLock {
            currentStart = null
            pausedSince = null
        }
    }

    suspend fun startSong() {
        mutex.withLock {
            currentStart = Instant.now()
            pausedSince = null
        }
    }

    suspend fun startPause() {
        mutex.withLock {
            if (pausedSince == null && currentStart != null) {
                pausedSince = Instant.now()
            }
        }
    }

    suspend fun stopPause() {
        mutex.withLock {
            val currentStart = currentStart
            val pausedSince = pausedSince
            if (currentStart == null || pausedSince == null) {
                return
            }
            val pauseDuration = Duration.between(pausedSince, Instant.now())
            this.currentStart = currentStart.plus(pauseDuration)
        }
    }

    suspend fun updateProgress(progress: Duration) {
        mutex.withLock {
            currentStart = Instant.now().minus(progress)
        }
    }
}

data class Progress(val progress: Duration, val isPaused: Boolean) {

}
