package net.bjoernpetersen.musicbot.api.plugin.volume

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provides
import kotlinx.coroutines.runBlocking
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import net.bjoernpetersen.musicbot.spi.plugin.volume.VolumeHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

class VolumeManagerTest {

    private fun injector(bound: Boolean): Injector = Guice
        .createInjector(if (bound) listOf(HandlerModule()) else emptyList())

    @Test
    fun isSingleton() {
        val injector = injector(false)
        assertSame(injector.manager, injector.manager)
    }

    @Test
    fun unboundGet() = runBlocking<Unit> {
        val manager = injector(false).manager
        assertThat(manager.getVolume())
            .isEqualTo(Volume())
            .returns(false, Volume::isSupported)
    }

    @TestFactory
    fun unboundSet(): List<DynamicTest> {
        val manager = injector(false).manager
        return listOf(0, 50, 100)
            .map {
                dynamicTest(it.toString()) {
                    assertDoesNotThrow {
                        runBlocking { manager.setVolume(it) }
                    }
                }
            }
    }

    @Test
    fun volumeTooLow() = runBlocking<Unit> {
        val injector = injector(false)
        assertThrows<IllegalArgumentException> {
            runBlocking { injector.manager.setVolume(-1) }
        }
    }

    @Test
    fun volumeTooHigh() = runBlocking<Unit> {
        val injector = injector(false)
        assertThrows<IllegalArgumentException> {
            runBlocking { injector.manager.setVolume(101) }
        }
    }

    @Test
    fun boundGet() = runBlocking<Unit> {
        val injector = injector(true)
        val handler = injector.handler
        val manager = injector.manager
        assertThat(manager.getVolume())
            .isEqualTo(Volume(handler.getVolume()))
            .returns(true, Volume::isSupported)
    }

    @Test
    fun boundSet() = runBlocking {
        val injector = injector(true)
        val manager = injector.manager

        val expectedVolume = Volume(50)
        assertNotEquals(expectedVolume, manager.getVolume())
        manager.setVolume(expectedVolume.volume)

        val handler = injector.handler
        assertEquals(expectedVolume, manager.getVolume())
        assertEquals(expectedVolume.volume, handler.getVolume())
    }
}

private val Injector.handler: VolumeHandler
    get() = getInstance(VolumeHandler::class.java)

private val Injector.manager: VolumeManager
    get() = getInstance(VolumeManager::class.java)

@Suppress("unused")
private class HandlerModule : AbstractModule() {

    @Provides
    @Singleton
    fun provideHandler(): VolumeHandler = TestHandler()
}

private class TestHandler : VolumeHandler {
    private var volume: Int = 0
    override val name: String
        get() = TODO("not implemented")
    override val description: String
        get() = TODO("not implemented")

    override suspend fun getVolume(): Int = volume

    override suspend fun setVolume(value: Int) {
        volume = value
    }

    override fun createConfigEntries(config: Config): List<Config.Entry<*>> {
        TODO("not implemented")
    }

    override fun createSecretEntries(secrets: Config): List<Config.Entry<*>> {
        TODO("not implemented")
    }

    override fun createStateEntries(state: Config) {
        TODO("not implemented")
    }

    override suspend fun initialize(initStateWriter: InitStateWriter) {
        TODO("not implemented")
    }

    override suspend fun close() {
        TODO("not implemented")
    }
}
