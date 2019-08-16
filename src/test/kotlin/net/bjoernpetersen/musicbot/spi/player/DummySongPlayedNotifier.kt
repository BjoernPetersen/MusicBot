package net.bjoernpetersen.musicbot.spi.player

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Module
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.player.SongEntry

class DummySongPlayedNotifier @Inject private constructor(
    @Inject(optional = true) private val callback: Callback?
) : SongPlayedNotifier {
    private val logger = KotlinLogging.logger {}

    override suspend fun notifyPlayed(songEntry: SongEntry) {
        logger.debug { "Notify played: $songEntry" }
        callback?.callback(songEntry)
    }

    interface Callback {
        suspend fun callback(songEntry: SongEntry)
    }

    companion object : AbstractModule() {
        override fun configure() {
            bind(SongPlayedNotifier::class.java).to(DummySongPlayedNotifier::class.java)
        }
    }
}

private fun callback(
    notify: suspend (SongEntry) -> Unit
): DummySongPlayedNotifier.Callback = object : DummySongPlayedNotifier.Callback {
    override suspend fun callback(songEntry: SongEntry) {
        notify(songEntry)
    }
}

fun songNotifierCallback(
    notify: suspend (SongEntry) -> Unit
): Module = object : AbstractModule() {
    private val callback = callback(notify)
    override fun configure() {
        bind(DummySongPlayedNotifier.Callback::class.java).toInstance(callback)
    }
}
