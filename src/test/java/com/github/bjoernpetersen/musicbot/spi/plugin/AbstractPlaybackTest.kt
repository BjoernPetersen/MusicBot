package com.github.bjoernpetersen.musicbot.spi.plugin

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.Duration.ofMillis
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

internal class AbstractPlaybackTest {
    @Test
    fun waitIndefinitely() {
        val uut = TestPlayback()
        Assertions.assertThrows(AssertionError::class.java) {
            assertTimeoutPreemptively(ofMillis(500)) { uut.waitForFinish() }
        }
    }

    @TestFactory
    fun waitFinishesOnDone(): List<DynamicTest> {
        return DoneCall.values().map { doneCall ->
            dynamicTest(doneCall.name) {
                val uut = TestPlayback()
                val lock = ReentrantLock()
                val success = AtomicBoolean(false)
                val called = lock.newCondition()
                val waiter = Thread {
                    try {
                        uut.waitForFinish()
                        success.set(true)
                    } catch (e: InterruptedException) {
                        // ignore, leave success on false
                    } finally {
                        lock.lock()
                        try {
                            called.signalAll()
                        } finally {
                            lock.unlock()
                        }
                    }
                }
                waiter.start()

                lock.lock()
                try {
                    assertTimeoutPreemptively(ofMillis(500)) { doneCall.call(uut) }
                    assertTrue(called.await(500, TimeUnit.MILLISECONDS))
                } finally {
                    lock.unlock()
                    waiter.interrupt()
                }

                assertTrue(success.get())
            }
        }
    }

    private class TestPlayback : AbstractPlayback() {
        override fun play() {}
        override fun pause() {}
    }
}
