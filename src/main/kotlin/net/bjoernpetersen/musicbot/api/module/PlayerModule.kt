package net.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import net.bjoernpetersen.musicbot.api.player.DefaultSuggester
import net.bjoernpetersen.musicbot.internal.player.PlayerHistoryImpl
import net.bjoernpetersen.musicbot.spi.player.PlayerHistory
import net.bjoernpetersen.musicbot.spi.plugin.Suggester

abstract class PlayerModule(private val suggester: Suggester?) : AbstractModule() {
    override fun configure() {
        bind(DefaultSuggester::class.java).toInstance(DefaultSuggester(suggester))
        bind(PlayerHistory::class.java).to(PlayerHistoryImpl::class.java).`in`(Scopes.SINGLETON)
    }
}
