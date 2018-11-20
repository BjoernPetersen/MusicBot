package com.github.bjoernpetersen.musicbot.api.module

import com.github.bjoernpetersen.musicbot.api.player.DefaultSuggester
import com.github.bjoernpetersen.musicbot.spi.plugin.Suggester
import com.google.inject.AbstractModule

abstract class PlayerModule(private val suggester: Suggester?) : AbstractModule() {
    override fun configure() {
        bind(DefaultSuggester::class.java).toInstance(DefaultSuggester(suggester))
    }
}
