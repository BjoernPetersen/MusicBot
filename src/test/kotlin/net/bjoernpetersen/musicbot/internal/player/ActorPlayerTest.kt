package net.bjoernpetersen.musicbot.internal.player

import com.google.inject.Injector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import name.falgout.jeffrey.testing.junit.guice.GuiceExtension
import name.falgout.jeffrey.testing.junit.guice.IncludeModule
import net.bjoernpetersen.musicbot.api.auth.BotUser
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.loader.NoResource
import net.bjoernpetersen.musicbot.api.module.DefaultPlayerModule
import net.bjoernpetersen.musicbot.api.module.DefaultQueueModule
import net.bjoernpetersen.musicbot.api.player.ErrorState
import net.bjoernpetersen.musicbot.api.player.PauseState
import net.bjoernpetersen.musicbot.api.player.PlayState
import net.bjoernpetersen.musicbot.api.player.PlayerState
import net.bjoernpetersen.musicbot.api.player.QueueEntry
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.player.StopState
import net.bjoernpetersen.musicbot.api.player.song
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.loader.DummyResourceCache
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.player.DummySongPlayedNotifier
import net.bjoernpetersen.musicbot.spi.player.Player
import net.bjoernpetersen.musicbot.spi.player.SongQueue
import net.bjoernpetersen.musicbot.spi.player.songNotifierCallback
import net.bjoernpetersen.musicbot.spi.plugin.AbstractPlayback
import net.bjoernpetersen.musicbot.spi.plugin.BrokenSuggesterException
import net.bjoernpetersen.musicbot.spi.plugin.DummyPluginLookup
import net.bjoernpetersen.musicbot.spi.plugin.NoSuchSongException
import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.assertj.core.api.ObjectAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

@ExtendWith(GuiceExtension::class)
@IncludeModule(DefaultQueueModule::class, DummyResourceCache.Companion::class)
class ActorPlayerTest {
    private val provider = DummyProvider()

    private fun Injector.createPlayer(suggester: Suggester? = null): Player {
        return createChildInjector(
            DefaultPlayerModule(suggester),
            DummyPluginLookup(provider),
            DummySongPlayedNotifier,
            songNotifierCallback { suggester?.notifyPlayed(it) }
        ).getInstance(Player::class.java)
    }

    @AfterEach
    fun clearQueue(queue: SongQueue) {
        queue.clear()
    }

    @Test
    fun pause(injector: Injector) {
        val player = injector.createPlayer(AlternatingSuggester(provider))
        runBlocking {
            player.start()
            player.next()
            val preState = player.state
            assertThat(preState)
                .asInstanceOf<PlayState>()
            player.pause()
            assertThat(player.state)
                .asInstanceOf<PauseState>()
                .extracting { it.entry }
                .isEqualTo(preState.entry)

            player.close()
        }
    }

    @Test
    fun unpause(injector: Injector) {
        val player = injector.createPlayer(AlternatingSuggester(provider))
        runBlocking {
            player.start()
            player.next()
            player.pause()
            val preState = player.state
            assertThat(preState)
                .asInstanceOf<PauseState>()

            player.play()
            assertThat(player.state)
                .asInstanceOf<PlayState>()
                .extracting { it.entry }
                .isEqualTo(preState.entry)

            player.close()
        }
    }

    @TestFactory
    fun next(injector: Injector): List<DynamicTest> {
        return listOf<Pair<String, suspend Player.(AlternatingSuggester) -> Unit>>(
            "in PlayState" to {
                next()
                assertThat(state)
                    .asInstanceOf<PlayState>()
                    .returns(provider.component1()) { it.entry.song }
            },
            "in PauseState" to {
                next()
                pause()
                assertThat(state)
                    .asInstanceOf<PauseState>()
                    .returns(provider.component1()) { it.entry.song }
            },
            "in StopState" to {
                assertThat(state)
                    .asInstanceOf<StopState>()
            },
            "in ErrorState" to {
                it.isBroken = true
                next()
                assertThat(state).asInstanceOf<ErrorState>()
                it.isBroken = false
            }
        ).map { (name, configure) ->
            dynamicTest(name) {
                val suggester = AlternatingSuggester(provider)
                val player = injector.createPlayer(suggester)
                runBlocking {
                    player.configure(suggester)
                    val before = player.state.entry?.song
                    player.next()
                    assertThat(player.state)
                        .asInstanceOf<PlayState>()
                        .extracting { it.entry.song }
                        .isNotEqualTo(before)
                    player.close()
                }
            }
        }
    }

    @Test
    fun `many next calls`(injector: Injector, queue: SongQueue) = runBlocking<Unit> {
        val player = injector.createPlayer()
        queue.insert(QueueEntry(provider.component1(), BotUser))
        queue.insert(QueueEntry(provider.component2(), BotUser))
        withContext(Dispatchers.Default) {
            coroutineScope {
                repeat(1000) {
                    launch {
                        player.next()
                    }
                }
            }
        }
        assertEquals(provider.component1(), player.state.entry?.song) {
            "Many (almost) simultaneous next calls should be handled as one"
        }
        player.close()
    }

    @Nested
    inner class AutoNext {
        @Test
        fun `without suggester`(injector: Injector) {
            val player = injector.createPlayer()

            runBlocking {
                player.start()
                delay(DummyProvider.LOADING_TIME.plusMillis(100).toMillis())
                assertThat(player.state)
                    .isSameAs(StopState)
                player.close()
            }
        }

        @Test
        fun `in StopState`(injector: Injector) {
            val suggester = AlternatingSuggester(provider)
            val player = injector.createPlayer(suggester)
            runBlocking {
                player.start()
                delay(DummyProvider.LOADING_TIME.plusMillis(100).toMillis())
                assertThat(player.state)
                    .asInstanceOf<PlayState>()
                player.close()
            }
        }

        @Test
        fun `in PlayState`(injector: Injector) {
            val suggester = AlternatingSuggester(provider)
            val player = injector.createPlayer(suggester)
            runBlocking {
                player.next()
                val song = player.state.entry?.song
                assertThat(player.state)
                    .asInstanceOf<PlayState>()

                player.start()
                delay(DummyProvider.DURATION.plusSeconds(1).toMillis())
                assertNotEquals(song, player.state.entry?.song)
                player.close()
            }
        }
    }

    @Nested
    inner class Listener {
        @Test
        fun pause(injector: Injector) {
            val player = injector.createPlayer(AlternatingSuggester(provider))
            runBlocking {
                player.start()
                player.next()
                val preState = player.state
                val changes = ArrayList<Pair<PlayerState, PlayerState>>()
                player.addListener { oldState, newState ->
                    changes.add(oldState to newState)
                }
                player.pause()

                var prev = preState
                for ((old, new) in changes) {
                    assertEquals(prev, old)
                    assertNotEquals(old, new)
                    prev = new
                }

                assertThat(changes.last().second)
                    .asInstanceOf<PauseState>()
                    .extracting { it.entry }
                    .isEqualTo(preState.entry)

                player.close()
            }
        }

        @Test
        fun unpause(injector: Injector) {
            val player = injector.createPlayer(AlternatingSuggester(provider))
            runBlocking {
                player.start()
                player.next()
                player.pause()
                val preState = player.state
                val changes = ArrayList<Pair<PlayerState, PlayerState>>()
                player.addListener { oldState, newState ->
                    changes.add(oldState to newState)
                }
                player.play()

                var prev = preState
                for ((old, new) in changes) {
                    assertEquals(prev, old)
                    assertNotEquals(old, new)
                    prev = new
                }

                assertThat(changes.last().second)
                    .asInstanceOf<PlayState>()
                    .extracting { it.entry }
                    .isEqualTo(preState.entry)

                player.close()
            }
        }

        @Test
        fun next(injector: Injector) {
            val suggester: Suggester = AlternatingSuggester(provider)
            val player = injector.createPlayer(suggester)
            runBlocking {
                player.start()
                player.next()
                val preState = player.state
                val changes = ArrayList<Pair<PlayerState, PlayerState>>()
                player.addListener { oldState, newState ->
                    changes.add(oldState to newState)
                }

                val nextSong = suggester.suggestNext()
                player.next()

                var prev = preState
                for ((old, new) in changes) {
                    assertEquals(prev, old)
                    assertNotEquals(old, new)
                    prev = new
                }

                assertThat(changes.last().second)
                    .asInstanceOf<PlayState>()
                    .extracting { it.entry.song }
                    .isEqualTo(nextSong)

                player.close()
            }
        }
    }
    // TODO test feedback channel handling
}

private inline fun <reified U> ObjectAssert<*>.asInstanceOf(): ObjectAssert<U> {
    return asInstanceOf(InstanceOfAssertFactories.type(U::class.java))
}

@IdBase("Alternating")
private class AlternatingSuggester(private val provider: DummyProvider) : Suggester {
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
    override suspend fun initialize(initStateWriter: InitStateWriter) = Unit
    override suspend fun close() {
        isBroken = true
    }
}

@IdBase("Dummy")
private class DummyProvider : Provider {
    override val name: String = "Dummy"
    override val subject: String
        get() = name
    override val description: String
        get() = name

    val songs = listOf(createSong("one"), createSong("two"))

    operator fun component1(): Song = songs[0]
    operator fun component2(): Song = songs[1]

    operator fun get(index: Int) = songs[index]

    override suspend fun search(query: String, offset: Int): List<Song> {
        return songs.filter { it.id in query }
    }

    override suspend fun lookup(id: String): Song {
        return songs.firstOrNull { it.id == id } ?: throw NoSuchSongException(id)
    }

    override suspend fun supplyPlayback(song: Song, resource: Resource): Playback = DummyPlayback()

    override suspend fun loadSong(song: Song): Resource = NoResource
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

    private class DummyPlayback : AbstractPlayback() {
        private var started = false
        override suspend fun play() {
            if (!started) {
                delay(LOADING_TIME.toMillis())
                launch {
                    delay(DURATION.toMillis())
                    markDone()
                }
                started = true
            }
        }

        override suspend fun pause() = Unit

        override suspend fun close() {
            delay(CLOSING_TIME.toMillis())
            super.close()
        }
    }

    companion object {
        val DURATION: Duration = Duration.ofSeconds(10)
        val LOADING_TIME: Duration = Duration.ofSeconds(1)
        val CLOSING_TIME: Duration = Duration.ofMillis(500)
    }
}
