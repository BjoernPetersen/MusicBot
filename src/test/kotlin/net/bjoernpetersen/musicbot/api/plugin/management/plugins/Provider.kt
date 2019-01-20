package net.bjoernpetersen.musicbot.api.plugin.management.plugins

import net.bjoernpetersen.musicbot.api.Song
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackSupplier
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import javax.inject.Inject

@IdBase
class SelfIdProvider : Provider by TodoProvider("self")

@IdBase
interface MyProvider : Provider

class MyProviderImpl : Provider by TodoProvider("My"), MyProvider

class AuthMyProvider : Provider by TodoProvider("AuthMy"), MyProvider {
    @Inject
    private lateinit var auth: DumbAuth
}

private class TodoProvider(override val name: String) : Provider {
    override fun search(query: String, offset: Int): List<Song> {
        TODO("not implemented")
    }

    override fun lookup(id: String): Song {
        TODO("not implemented")
    }

    override fun getPlaybackSupplier(song: Song): PlaybackSupplier {
        TODO("not implemented")
    }

    override fun loadSong(song: Song): Boolean {
        TODO("not implemented")
    }

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

    override val subject: String
        get() = TODO("not implemented")
}
