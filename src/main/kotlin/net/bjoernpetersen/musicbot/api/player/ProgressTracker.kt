package net.bjoernpetersen.musicbot.api.player

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressTracker @Inject private constructor() {

    private val logger = KotlinLogging.logger {}

    private val mutex = Mutex()

    private var currentStart: Instant? = null
    private var pausedSince: Instant? = null

    suspend fun getCurrentProgress(): Progress {
        mutex.withLock {
            logger.debug { "Get progress" }
            val currentStart = currentStart ?: return Progress(Duration.ZERO, true)
            val pausedSince = pausedSince
            return if (pausedSince == null)
                Progress(Duration.between(currentStart, Instant.now()), false)
            else
                Progress(Duration.between(currentStart, pausedSince), true)
        }
    }

    suspend fun reset() {
        mutex.withLock {
            logger.debug { "Reset" }
            currentStart = null
            pausedSince = null
        }
    }

    suspend fun startSong() {
        mutex.withLock {
            logger.debug { "Start" }
            currentStart = Instant.now()
            pausedSince = null
        }
    }

    suspend fun startPause() {
        mutex.withLock {
            logger.debug { "Pause" }
            if (pausedSince == null && currentStart != null) {
                pausedSince = Instant.now()
            }
        }
    }

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

    suspend fun updateProgress(progress: Duration) {
        mutex.withLock {
            currentStart = Instant.now().minus(progress)
        }
    }
}

data class Progress(val duration: Duration, val isPaused: Boolean) {

}
