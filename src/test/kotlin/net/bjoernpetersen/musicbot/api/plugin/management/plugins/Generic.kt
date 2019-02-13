package net.bjoernpetersen.musicbot.api.plugin.management.plugins

import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.plugin.ActiveBase
import net.bjoernpetersen.musicbot.api.plugin.Base
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter

@IdBase("Self ID")
@ActiveBase
class SelfIdActiveGeneric : GenericPlugin by TodoGeneric("self active")

class SelfIdGeneric : GenericPlugin by TodoGeneric("self inactive")

@IdBase("Separate ID")
@ActiveBase
interface ActiveGenericId : GenericPlugin

class ActiveGeneric : GenericPlugin by TodoGeneric("active"), ActiveGenericId

@Base
interface DumbAuth : GenericPlugin {

    fun token(): String
}

class DumbAuthImpl : GenericPlugin by TodoGeneric("DumbAuth"), DumbAuth {
    override fun token(): String {
        return "password"
    }
}

private class TodoGeneric(override val name: String) : GenericPlugin {
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
