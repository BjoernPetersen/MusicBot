package net.bjoernpetersen.musicbot.api.player

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import name.falgout.jeffrey.testing.junit.guice.GuiceExtension
import net.bjoernpetersen.musicbot.api.player.SongProgressAssert.Companion.assertThat
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

@Suppress("RemoveExplicitTypeArguments")
@ExtendWith(GuiceExtension::class)
class ProgressTrackerTest {

    @BeforeEach
    fun resetTracker(tracker: ProgressTracker) = runBlocking<Unit> {
        tracker.reset()
    }

    @Test
    fun startSong(tracker: ProgressTracker) = runBlocking<Unit> {
        tracker.startSong()
        delay(200)
        assertThat(tracker.getCurrentProgress())
            .isPlaying()
            .isNearlyEqual(Duration.ofMillis(200))
    }

    @Test
    fun updateProgress(tracker: ProgressTracker) = runBlocking<Unit> {
        tracker.updateProgress(Duration.ofSeconds(1))
        assertThat(tracker.getCurrentProgress())
            .isPaused()
            .isNearlyEqual(Duration.ZERO)
    }

    @Test
    fun startPause(tracker: ProgressTracker) = runBlocking<Unit> {
        // No assertions, just looking for errors
        tracker.startPause()
    }

    @Test
    fun stopPause(tracker: ProgressTracker) = runBlocking<Unit> {
        tracker.stopPause()
        // Nothing is playing, so we can't unpause
        assertThat(tracker.getCurrentProgress())
            .isPaused()
            .isNearlyEqual(Duration.ZERO)
    }

    @Nested
    inner class Started {

        @BeforeEach
        fun start(tracker: ProgressTracker) = runBlocking<Unit> {
            tracker.startSong()
            delay(500)
        }

        @Test
        fun startSong(tracker: ProgressTracker) = runBlocking<Unit> {
            tracker.startSong()
            delay(100)
            assertThat(tracker.getCurrentProgress())
                .isNearlyEqual(Duration.ofMillis(100))
        }

        @Test
        fun updateProgress(tracker: ProgressTracker) = runBlocking<Unit> {
            val progress = Duration.ofMillis(6000)
            tracker.updateProgress(progress)

            assertThat(tracker.getCurrentProgress())
                .isPlaying()
                .isNearlyEqual(progress)
        }

        @Test
        fun `updateProgress negative duration`(tracker: ProgressTracker) = runBlocking<Unit> {
            tracker.updateProgress(Duration.ofSeconds(-2))

            assertThat(tracker.getCurrentProgress())
                .isPlaying()
                .isNearlyEqual(Duration.ZERO)
        }

        @Test
        fun startPause(tracker: ProgressTracker) = runBlocking<Unit> {
            tracker.startPause()
            assertThat(tracker.getCurrentProgress())
                .isPaused()
        }

        @Test
        fun stopPause(tracker: ProgressTracker) = runBlocking<Unit> {
            // No assertions, just looking for errors
            tracker.stopPause()
        }

        @Test
        fun reset(tracker: ProgressTracker) = runBlocking<Unit> {
            tracker.reset()
            assertThat(tracker.getCurrentProgress())
                .isPaused()
                .isNearlyEqual(Duration.ZERO)
        }

        @Nested
        inner class Paused {

            private lateinit var pausedTime: Duration

            @BeforeEach
            fun `start and pause`(tracker: ProgressTracker) = runBlocking<Unit> {
                tracker.startPause()
                pausedTime = tracker.getCurrentProgress().duration
            }

            @Test
            fun startSong(tracker: ProgressTracker) = runBlocking<Unit> {
                tracker.startSong()
                delay(100)
                assertThat(tracker.getCurrentProgress())
                    .isNearlyEqual(Duration.ofMillis(100))
                    .isPlaying()
            }

            @Test
            fun `is actually paused`(tracker: ProgressTracker) = runBlocking<Unit> {
                val progress = tracker.getCurrentProgress()
                delay(300)
                assertEquals(progress.duration, tracker.getCurrentProgress().duration)
            }

            @Test
            fun updateProgress(tracker: ProgressTracker) = runBlocking<Unit> {
                val progress = Duration.ofMillis(5500)
                tracker.updateProgress(progress)

                assertThat(tracker.getCurrentProgress())
                    .isNearlyEqual(progress)
                    .isPaused()
            }

            @Test
            fun `updateProgress negative duration`(tracker: ProgressTracker) = runBlocking<Unit> {
                tracker.updateProgress(Duration.ofSeconds(-2))

                assertThat(tracker.getCurrentProgress())
                    .isPaused()
                    .isNearlyEqual(Duration.ZERO)
            }

            @Test
            fun stopPause(tracker: ProgressTracker) = runBlocking<Unit> {
                val progress = tracker.getCurrentProgress()
                tracker.stopPause()

                assertThat(tracker.getCurrentProgress())
                    .isPlaying()
                    .isNearlyEqual(progress.duration)

                delay(200)
                assertThat(tracker.getCurrentProgress())
                    .isPlaying()
                    .isNearlyEqual(progress.duration.plus(Duration.ofMillis(200)))
            }

            @Test
            fun reset(tracker: ProgressTracker) = runBlocking<Unit> {
                tracker.reset()
                assertThat(tracker.getCurrentProgress())
                    .isPaused()
                    .isNearlyEqual(Duration.ZERO)
            }
        }
    }
}

private class SongProgressAssert private constructor(
    progress: SongProgress
) : AbstractObjectAssert<SongProgressAssert, SongProgress>(
    progress,
    SongProgressAssert::class.java
) {

    fun isNearlyEqual(other: Duration, delta: Duration = DELTA): SongProgressAssert = apply {
        assertThat(actual.duration)
            .isBetween(other.minus(delta), other.plus(delta))
    }

    fun isPaused(): SongProgressAssert = apply {
        assertThat(actual.isPaused).isTrue()
    }

    fun isPlaying(): SongProgressAssert = apply {
        assertThat(actual.isPaused).isFalse()
    }

    companion object {
        private val DELTA = Duration.ofMillis(100)

        @JvmStatic
        fun assertThat(progress: SongProgress): SongProgressAssert {
            return SongProgressAssert(progress)
        }
    }
}
