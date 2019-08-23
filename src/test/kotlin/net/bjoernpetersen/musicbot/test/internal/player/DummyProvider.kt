package net.bjoernpetersen.musicbot.test.internal.player

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.loader.NoResource
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.player.song
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.plugin.AbstractPlayback
import net.bjoernpetersen.musicbot.spi.plugin.NoSuchSongException
import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import java.time.Duration

@IdBase("Dummy")
class DummyProvider : Provider {
    override val name: String = "Dummy"
    override val subject: String
        get() = name
    override val description: String
        get() = name

    val songs = listOf(createSong("one"), createSong("two"))

    var loadingTime: Duration = LOADING_TIME
    var closingTime: Duration = CLOSING_TIME

    fun resetTimes() {
        loadingTime = LOADING_TIME
        closingTime = CLOSING_TIME
    }

    operator fun component1(): Song = songs[0]
    operator fun component2(): Song = songs[1]

    operator fun get(index: Int) = songs[index]

    override suspend fun search(query: String, offset: Int): List<Song> {
        return songs.filter { it.id in query }
    }

    override suspend fun lookup(id: String): Song {
        return songs.firstOrNull { it.id == id } ?: throw NoSuchSongException(
            id
        )
    }

    override suspend fun supplyPlayback(song: Song, resource: Resource): Playback =
        DummyPlayback()

    override suspend fun loadSong(song: Song): Resource =
        NoResource

    override fun createConfigEntries(config: Config): List<Config.Entry<*>> = emptyList()
    override fun createSecretEntries(secrets: Config): List<Config.Entry<*>> = emptyList()
    override fun createStateEntries(state: Config) = Unit
    override suspend fun initialize(initStateWriter: InitStateWriter) = Unit
    override suspend fun close() = Unit

    private fun createSong(id: String): Song = song(id) {
        title = "title-$id"
        description = "description-$id"
        duration = DURATION.seconds.toInt()
    }

    private inner class DummyPlayback : AbstractPlayback() {
        private var started = false
        override suspend fun play() {
            if (!started) {
                delay(loadingTime.toMillis())
                launch {
                    delay(DURATION.toMillis())
                    markDone()
                }
                started = true
            }
        }

        override suspend fun pause() = Unit

        override suspend fun close() {
            delay(closingTime.toMillis())
            super.close()
        }
    }

    companion object {
        val DURATION: Duration = Duration.ofSeconds(5)
        val LOADING_TIME: Duration = Duration.ofMillis(500)
        val CLOSING_TIME: Duration = Duration.ofMillis(200)
    }
}
