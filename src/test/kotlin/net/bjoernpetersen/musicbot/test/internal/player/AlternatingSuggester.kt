package net.bjoernpetersen.musicbot.test.internal.player

import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.plugin.BrokenSuggesterException
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.management.ProgressFeedback

@IdBase("Alternating")
class AlternatingSuggester(private val provider: DummyProvider) :
    Suggester {
    private val logger = KotlinLogging.logger { }

    override val name: String = "Alternating"
    override val subject: String
        get() = name
    override val description: String
        get() = name

    var isBroken = false
    private var currentIndex: Int = 0

    private fun nextIndex(currentIndex: Int): Int = (currentIndex + 1) % provider.songs.size

    override suspend fun getNextSuggestions(maxLength: Int): List<Song> {
        if (isBroken) throw BrokenSuggesterException()
        return generateSequence(currentIndex, ::nextIndex)
            .take(provider.songs.size)
            .take(maxLength)
            .map { provider[it] }
            .toList()
    }

    override suspend fun suggestNext(): Song {
        if (isBroken) throw BrokenSuggesterException()
        return provider[currentIndex]
    }

    override suspend fun removeSuggestion(song: Song) {
        if (song == provider[currentIndex]) {
            currentIndex = nextIndex(currentIndex)
        }
    }

    override fun createConfigEntries(config: Config): List<Config.Entry<*>> = emptyList()
    override fun createSecretEntries(secrets: Config): List<Config.Entry<*>> = emptyList()
    override fun createStateEntries(state: Config) = Unit
    override suspend fun initialize(progressFeedback: ProgressFeedback) = Unit
    override suspend fun close() {
        isBroken = true
    }
}
