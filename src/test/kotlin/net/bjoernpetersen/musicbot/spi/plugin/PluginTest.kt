package net.bjoernpetersen.musicbot.spi.plugin

import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.api.plugin.bases
import net.bjoernpetersen.musicbot.api.plugin.id
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PluginTest {
    @Test
    fun allBases() {
        val uut = TestProvider()
        val bases = uut.bases.toSet()
        assertEquals(setOf(Provider::class, TestProviderBase::class), bases)
    }

    @Test
    fun idDoesNotThrow() {
        val uut = TestProvider()
        assertDoesNotThrow { uut.id }
    }

    @Test
    fun idCorrect() {
        val uut = TestProvider()
        assertEquals(TestProviderBase::class, uut.id.type)
    }
}

@IdBase("Test")
private interface TestProviderBase : Provider

private class TestProvider : TestProviderBase {
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

    override val subject: String = "For testing eyes only"
    override val description: String = ""
    override val name: String = "TestProvider"

    override suspend fun loadSong(song: Song): Resource {
        TODO("not implemented")
    }

    override suspend fun supplyPlayback(song: Song, resource: Resource): Playback {
        TODO("not implemented")
    }

    override suspend fun lookup(id: String): Song {
        TODO("not implemented")
    }

    override suspend fun search(query: String, offset: Int): List<Song> {
        TODO("not implemented")
    }
}
