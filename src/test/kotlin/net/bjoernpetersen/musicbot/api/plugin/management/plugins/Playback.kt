package net.bjoernpetersen.musicbot.api.plugin.management.plugins

import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.plugin.Base
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter

@IdBase
class SelfIdPlayback : PlaybackFactory by TodoPlaybackFactory("self")

@Base
interface EchoPlayback : PlaybackFactory {

    fun echoThis(message: String)
}

@IdBase
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

    override fun initialize(initStateWriter: InitStateWriter) {
        TODO("not implemented")
    }

    override fun close() {
        TODO("not implemented")
    }
}
