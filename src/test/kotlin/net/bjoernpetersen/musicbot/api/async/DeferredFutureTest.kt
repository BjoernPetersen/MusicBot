package net.bjoernpetersen.musicbot.api.async

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class DeferredFutureTest {
    @Test
    fun `get succeeds`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async {
            delay(50)
            VALUE
        }
        val future = deferred.asFuture()
        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertEquals(VALUE, future.get())
        }
    }

    @Test
    fun `get throws ExecutionException`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async {
            throw TestException()
        }
        val future = deferred.asFuture()
        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertThrows<ExecutionException> {
                future.get()
            }
        }
    }

    @Test
    fun `get throws CancellationException for direct cancel`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async {
            delay(500)
        }
        val future = deferred.asFuture()

        // Cancel Deferred directly
        deferred.cancel()

        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertThrows<CancellationException> {
                future.get()
            }
        }
    }

    @Test
    fun `get throws no CancellationException for non-lazy future non-interrupting cancel`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async {
            delay(200)
        }
        val future = deferred.asFuture()

        // Cancel Future
        assertFalse(future.cancel(false))

        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertDoesNotThrow {
                future.get()
            }
        }
    }

    @Test
    fun `get throws CancellationException for lazy future non-interrupting cancel`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async(start = CoroutineStart.LAZY) {
            delay(200)
        }
        val future = deferred.asFuture()

        // Cancel Future
        assertTrue(future.cancel(false))

        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertThrows<CancellationException> {
                future.get()
            }
        }
    }

    @Test
    fun `get throws CancellationException for future interrupting cancel`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async {
            delay(500)
        }
        val future = deferred.asFuture()

        // Cancel Future
        assertTrue(future.cancel(true))

        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertThrows<CancellationException> {
                future.get()
            }
        }
    }

    @Test
    fun `timed get succeeds`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async {
            delay(50)
            VALUE
        }
        val future = deferred.asFuture()
        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertEquals(VALUE, future.get(100, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun `timed get times out`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async {
            delay(5000)
            VALUE
        }
        val future = deferred.asFuture()
        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertThrows<TimeoutException> { future.get(50, TimeUnit.MILLISECONDS) }
        }
    }

    @Test
    fun `timed get throws CancellationException for direct cancel`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async {
            delay(500)
        }
        val future = deferred.asFuture()

        // Cancel Deferred directly
        deferred.cancel()

        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertThrows<CancellationException> {
                future.get(200, TimeUnit.MILLISECONDS)
            }
        }
    }

    @Test
    fun `timed get throws no CancellationException for non-lazy future non-interrupting cancel`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async {
            delay(200)
        }
        val future = deferred.asFuture()

        // Cancel Future
        assertFalse(future.cancel(false))

        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertDoesNotThrow {
                future.get(500, TimeUnit.MILLISECONDS)
            }
        }
    }

    @Test
    fun `timed get throws CancellationException for lazy future non-interrupting cancel`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async(start = CoroutineStart.LAZY) {
            delay(200)
        }
        val future = deferred.asFuture()

        // Cancel Future
        assertTrue(future.cancel(false))

        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertThrows<CancellationException> {
                future.get(500, TimeUnit.MILLISECONDS)
            }
        }
    }

    @Test
    fun `timed get throws CancellationException for future interrupting cancel`() {
        val scope = CoroutineScope(Dispatchers.Default)
        val deferred = scope.async {
            delay(500)
        }
        val future = deferred.asFuture()

        // Cancel Future
        assertTrue(future.cancel(true))

        assertTimeoutPreemptively(Duration.ofSeconds(1)) {
            assertThrows<CancellationException> {
                future.get(200, TimeUnit.MILLISECONDS)
            }
        }
    }

    private companion object {
        const val VALUE = "TEST_VALUE"
    }
}

private class TestException : Exception()
