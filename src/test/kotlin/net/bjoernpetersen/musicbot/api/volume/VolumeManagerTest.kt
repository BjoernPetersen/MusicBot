package net.bjoernpetersen.musicbot.api.volume

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provides
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.plugin.volume.Volume
import net.bjoernpetersen.musicbot.api.plugin.volume.VolumeManager
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import net.bjoernpetersen.musicbot.spi.plugin.volume.VolumeHandler
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
    fun unboundGet() {
        val manager = injector(false).manager
        assertEquals(Volume(), manager.getVolume())
    }

    @TestFactory
    fun unboundSet(): List<DynamicTest> {
        val manager = injector(false).manager
        return listOf(0, 50, 100)
            .map {
                dynamicTest(it.toString()) {
                    assertDoesNotThrow { manager.setVolume(it) }
                }
            }
    }

    @Test
    fun volumeTooLow() {
        val injector = injector(false)
        assertThrows<IllegalArgumentException> { injector.manager.setVolume(-1) }
    }

    @Test
    fun volumeTooHigh() {
        val injector = injector(false)
        assertThrows<IllegalArgumentException> { injector.manager.setVolume(101) }
    }

    @Test
    fun boundGet() {
        val injector = injector(true)
        val handler = injector.handler
        val manager = injector.manager
        val expectedVolume = Volume(handler.volume)
        assertEquals(expectedVolume, manager.getVolume())
    }

    @Test
    fun boundSet() {
        val injector = injector(true)
        val manager = injector.manager

        val expectedVolume = Volume(50)
        assertNotEquals(expectedVolume, manager.getVolume())
        manager.setVolume(expectedVolume.volume)

        val handler = injector.handler
        assertEquals(expectedVolume, manager.getVolume())
        assertEquals(expectedVolume.volume, handler.volume)
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
    override var volume: Int = 0
    override val name: String
        get() = TODO("not implemented")
    override val description: String
        get() = TODO("not implemented")

    override fun createConfigEntries(config: Config): List<Config.Entry<*>> {
        TODO("not implemented")
    }

    override fun createSecretEntries(secrets: Config): List<Config.Entry<*>> {
        TODO("not implemented")
    }

    override fun createStateEntries(state: Config) {
        TODO("not implemented")
    }

    override fun initialize(initStateWriter: InitStateWriter) {
        TODO("not implemented")
    }

    override fun close() {
        TODO("not implemented")
    }
}
