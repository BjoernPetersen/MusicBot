package net.bjoernpetersen.musicbot.api.plugin.management.plugins

import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.plugin.Base
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter

@Base
class SelfBasePlayback : PlaybackFactory by TodoPlaybackFactory("self")

@Base
interface EchoPlayback : PlaybackFactory {

    fun echoThis(message: String)
}

@Base
interface MyPlayback : EchoPlayback

class MyPlaybackImpl : MyPlayback, PlaybackFactory by TodoPlaybackFactory("MyEcho") {
    override fun echoThis(message: String) {
        println("Echo $message")
    }
}

private class TodoPlaybackFactory(override val name: String) : PlaybackFactory {
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

    override suspend fun initialize(initStateWriter: InitStateWriter) {
        TODO("not implemented")
    }

    override suspend fun close() {
        TODO("not implemented")
    }
}
