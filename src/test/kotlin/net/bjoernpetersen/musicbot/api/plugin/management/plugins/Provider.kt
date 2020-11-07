package net.bjoernpetersen.musicbot.api.plugin.management.plugins

import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.management.ProgressUpdater
import javax.inject.Inject

@IdBase("Self ID")
class SelfIdProvider : Provider by TodoProvider("self")

@IdBase("Separate ID")
interface MyProvider : Provider

class MyProviderImpl : Provider by TodoProvider("My"), MyProvider

class AuthMyProvider : Provider by TodoProvider("AuthMy"), MyProvider {
    @Inject
    private lateinit var auth: DumbAuth
}

private class TodoProvider(override val name: String) : Provider {
    override suspend fun search(query: String, offset: Int): List<Song> {
        TODO("not implemented")
    }

    override suspend fun lookup(id: String): Song {
        TODO("not implemented")
    }

    override suspend fun supplyPlayback(song: Song, resource: Resource): Playback {
        TODO("not implemented")
    }

    override suspend fun loadSong(song: Song): Resource {
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

    override suspend fun initialize(progressUpdater: ProgressUpdater) {
        TODO("not implemented")
    }

    override suspend fun close() {
        TODO("not implemented")
    }

    override val subject: String
        get() = TODO("not implemented")
}
