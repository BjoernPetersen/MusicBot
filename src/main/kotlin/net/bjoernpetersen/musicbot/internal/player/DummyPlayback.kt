package net.bjoernpetersen.musicbot.internal.player

import kotlinx.coroutines.delay
import net.bjoernpetersen.musicbot.spi.plugin.Playback

internal object DummyPlayback : Playback {
    override suspend fun play() {}

    override suspend fun pause() {}

    override suspend fun waitForFinish() {
        delay(1000)
    }

    override suspend fun close() {}
}
