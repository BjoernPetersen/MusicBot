package net.bjoernpetersen.musicbot.internal.player

import com.google.inject.Injector
import kotlinx.coroutines.runBlocking
import name.falgout.jeffrey.testing.junit.guice.GuiceExtension
import name.falgout.jeffrey.testing.junit.guice.IncludeModule
import net.bjoernpetersen.musicbot.api.auth.BotUser
import net.bjoernpetersen.musicbot.api.module.DefaultPlayerModule
import net.bjoernpetersen.musicbot.api.module.DefaultQueueModule
import net.bjoernpetersen.musicbot.api.player.QueueEntry
import net.bjoernpetersen.musicbot.api.player.SongEntry
import net.bjoernpetersen.musicbot.spi.player.Player
import net.bjoernpetersen.musicbot.spi.player.PlayerHistory
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

@ExtendWith(GuiceExtension::class)
@IncludeModule(DefaultQueueModule::class, DummyResourceCache.Companion::class)
class PlayerHistoryTest {
    private val provider = DummyProvider().also {
        it.loadingTime = Duration.ofMillis(20)
        it.closingTime = Duration.ofMillis(5)
    }

    private fun Injector.createChild(suggester: Suggester? = null): Injector = createChildInjector(
        DefaultPlayerModule(suggester),
        DummyPluginLookup(provider),
        DummySongPlayedNotifier,
        songNotifierCallback { suggester?.notifyPlayed(it) }
    )

    @Test
    fun `history gets collected`(injector: Injector, queue: SongQueue) {
        val childInjector = injector.createChild()
        val player = childInjector.getInstance(Player::class.java)
        val history = childInjector.getInstance(PlayerHistory::class.java)
        assertThat(history.getHistory()).isEmpty()

        runBlocking {
            queue.insert(QueueEntry(provider.component1(), BotUser))
            queue.insert(QueueEntry(provider.component2(), BotUser))
            player.next()
            player.next()

            val entries = history.getHistory()
            entries.forEach {
                assertThat(it)
                    .asInstanceOf<QueueEntry>()
                    .returns(BotUser, SongEntry::user)
            }

            assertEquals(
                listOf(provider.component1(), provider.component2()),
                entries.map { it.song }
            )
        }
    }

    @Test
    fun `negative limit`(injector: Injector) {
        val suggester: Suggester = AlternatingSuggester(provider)
        val childInjector = injector.createChild(suggester)
        val player = childInjector.getInstance(Player::class.java)
        val history = childInjector.getInstance(PlayerHistory::class.java)

        runBlocking {
            repeat(5) {
                player.next()
            }
            assertThrows<IllegalArgumentException> {
                history.getHistory(-1)
            }
        }
    }

    @Test
    fun `zero limit`(injector: Injector) {
        val suggester: Suggester = AlternatingSuggester(provider)
        val childInjector = injector.createChild(suggester)
        val player = childInjector.getInstance(Player::class.java)
        val history = childInjector.getInstance(PlayerHistory::class.java)

        runBlocking {
            repeat(5) {
                player.next()
            }
            assertThat(history.getHistory(0))
                .isEmpty()
        }
    }

    @Test
    fun `limit above max size`(injector: Injector) {
        val suggester: Suggester = AlternatingSuggester(provider)
        val childInjector = injector.createChild(suggester)
        val player = childInjector.getInstance(Player::class.java)
        val history = childInjector.getInstance(PlayerHistory::class.java)

        runBlocking {
            repeat(PlayerHistoryImpl.MAX_SIZE + 5) {
                player.next()
            }
            assertThat(history.getHistory(PlayerHistoryImpl.MAX_SIZE + 10))
                .hasSize(PlayerHistoryImpl.MAX_SIZE)
        }
    }

    @Test
    fun `limit above history size`(injector: Injector) {
        val suggester: Suggester = AlternatingSuggester(provider)
        val childInjector = injector.createChild(suggester)
        val player = childInjector.getInstance(Player::class.java)
        val history = childInjector.getInstance(PlayerHistory::class.java)

        runBlocking {
            repeat(5) {
                player.next()
            }
            assertThat(history.getHistory(10))
                .hasSize(5)
        }
    }
}
