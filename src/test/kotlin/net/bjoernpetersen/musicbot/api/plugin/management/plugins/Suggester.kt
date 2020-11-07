package net.bjoernpetersen.musicbot.api.plugin.management.plugins

import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.management.ProgressFeedback
import javax.inject.Inject

@IdBase("Authed")
class AuthMySuggester : Suggester by TodoSuggester("AuthMy") {

    @Inject
    private lateinit var provider: MyProvider
}

private class TodoSuggester(override val name: String) : Suggester {
    override suspend fun suggestNext(): Song {
        TODO("not implemented")
    }

    override suspend fun getNextSuggestions(maxLength: Int): List<Song> {
        TODO("not implemented")
    }

    override suspend fun removeSuggestion(song: Song) {
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

    override suspend fun initialize(progressFeedback: ProgressFeedback) {
        TODO("not implemented")
    }

    override suspend fun close() {
        TODO("not implemented")
    }

    override val subject: String
        get() = TODO("not implemented")
}
