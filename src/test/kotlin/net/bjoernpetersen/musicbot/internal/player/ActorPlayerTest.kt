package net.bjoernpetersen.musicbot.internal.player

import com.google.inject.Injector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import name.falgout.jeffrey.testing.junit.guice.GuiceExtension
import name.falgout.jeffrey.testing.junit.guice.IncludeModule
import net.bjoernpetersen.musicbot.api.auth.BotUser
import net.bjoernpetersen.musicbot.api.module.DefaultPlayerModule
import net.bjoernpetersen.musicbot.api.module.DefaultQueueModule
import net.bjoernpetersen.musicbot.api.player.ErrorState
import net.bjoernpetersen.musicbot.api.player.PauseState
import net.bjoernpetersen.musicbot.api.player.PlayState
import net.bjoernpetersen.musicbot.api.player.PlayerState
import net.bjoernpetersen.musicbot.api.player.QueueEntry
import net.bjoernpetersen.musicbot.api.player.StopState
import net.bjoernpetersen.musicbot.spi.player.Player
import net.bjoernpetersen.musicbot.spi.player.SongQueue
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.test.asInstanceOf
import net.bjoernpetersen.musicbot.test.internal.player.AlternatingSuggester
import net.bjoernpetersen.musicbot.test.internal.player.DummyProvider
import net.bjoernpetersen.musicbot.test.spi.loader.DummyResourceCache
import net.bjoernpetersen.musicbot.test.spi.player.DummySongPlayedNotifier
import net.bjoernpetersen.musicbot.test.spi.player.songNotifierCallback
import net.bjoernpetersen.musicbot.test.spi.plugin.DummyPluginLookup
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith

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
        val player = injector.createPlayer(
            AlternatingSuggester(
                provider
            )
        )
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
        val player = injector.createPlayer(
            AlternatingSuggester(
                provider
            )
        )
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
                val suggester =
                    AlternatingSuggester(provider)
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
                delay(DummyProvider.LOADING_TIME.plusMillis(100))
                assertThat(player.state)
                    .isSameAs(StopState)
                player.close()
            }
        }

        @Test
        fun `in StopState`(injector: Injector) {
            val suggester =
                AlternatingSuggester(provider)
            val player = injector.createPlayer(suggester)
            runBlocking {
                player.start()
                delay(DummyProvider.LOADING_TIME.plusMillis(100))
                assertThat(player.state)
                    .asInstanceOf<PlayState>()
                player.close()
            }
        }

        @Test
        fun `in PlayState`(injector: Injector) {
            val suggester =
                AlternatingSuggester(provider)
            val player = injector.createPlayer(suggester)
            runBlocking {
                player.next()
                val song = player.state.entry?.song
                assertThat(player.state)
                    .asInstanceOf<PlayState>()

                player.start()
                delay(
                    DummyProvider.DURATION
                        .plus(DummyProvider.CLOSING_TIME)
                        .plus(DummyProvider.LOADING_TIME)
                        .plusMillis(50)
                )
                assertNotEquals(song, player.state.entry?.song)
                player.close()
            }
        }
    }

    @Nested
    inner class Listener {
        @Test
        fun pause(injector: Injector) {
            val player = injector.createPlayer(
                AlternatingSuggester(
                    provider
                )
            )
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
            val player = injector.createPlayer(
                AlternatingSuggester(
                    provider
                )
            )
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
            val suggester: Suggester =
                AlternatingSuggester(provider)
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
