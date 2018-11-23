package net.bjoernpetersen.musicbot.api.module

import net.bjoernpetersen.musicbot.api.player.DefaultSuggester
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import com.google.inject.AbstractModule

abstract class PlayerModule(private val suggester: Suggester?) : AbstractModule() {
    override fun configure() {
        bind(DefaultSuggester::class.java).toInstance(DefaultSuggester(suggester))
    }
}
