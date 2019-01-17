package net.bjoernpetersen.musicbot.spi.plugin

import net.bjoernpetersen.musicbot.api.Song
import net.bjoernpetersen.musicbot.api.config.Config
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
        assertEquals(TestProviderBase::class, uut.id)
    }
}

@IdBase
private interface TestProviderBase : Provider {

}

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

    override fun initialize(initStateWriter: InitStateWriter) {
        TODO("not implemented")
    }

    override fun close() {
        TODO("not implemented")
    }

    override val subject: String = "For testing eyes only"
    override val description: String = ""
    override val name: String = "TestProvider"

    override fun loadSong(song: Song): Boolean {
        TODO("not implemented")
    }

    override fun getPlaybackSupplier(song: Song): PlaybackSupplier {
        TODO("not implemented")
    }

    override fun lookup(id: String): Song {
        TODO("not implemented")
    }

    override fun search(query: String, offset: Int): List<Song> {
        TODO("not implemented")
    }

}
