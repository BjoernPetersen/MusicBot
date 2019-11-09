package net.bjoernpetersen.musicbot.test.spi.plugin

import com.google.inject.AbstractModule
import com.google.inject.Module
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.plugin.id
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.PluginLookup
import net.bjoernpetersen.musicbot.spi.plugin.Provider

class DummyPluginLookup private constructor(private val provider: Provider?) : PluginLookup {
    private val logger = KotlinLogging.logger { }
    private val id: String? by lazy {
        provider?.id?.qualifiedName?.also {
            logger.debug { "Got ID: $it" }
        }
    }

    @Inject
    constructor() : this(null)

    override fun <T : Plugin> lookup(base: KClass<T>): T? {
        logger.debug { "lookup for base ${base.qualifiedName}" }
        return if (provider != null && base.isInstance(provider)) base.cast(provider)
        else null
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Plugin> lookup(id: String): T? {
        logger.debug { "lookup for ID $id" }
        return if (id == this.id) provider as T
        else null
    }

    private class DummyPluginLookupModule(
        private val provider: Provider? = null
    ) : AbstractModule() {
        override fun configure() {
            if (provider != null)
                bind(PluginLookup::class.java).toInstance(DummyPluginLookup(provider))
            else
                bind(PluginLookup::class.java).to(DummyPluginLookup::class.java)
        }
    }

    companion object : Module by DummyPluginLookupModule() {
        operator fun invoke(provider: Provider): Module = DummyPluginLookupModule(provider)
    }
}
