package net.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import net.bjoernpetersen.musicbot.api.player.DefaultSuggester
import net.bjoernpetersen.musicbot.internal.player.PlayerHistoryImpl
import net.bjoernpetersen.musicbot.spi.player.PlayerHistory
import net.bjoernpetersen.musicbot.spi.plugin.Suggester

/**
 * Abstract base module for player modules.
 *
 * This module binds [DefaultSuggester] and [PlayerHistory], but not the actual player.
 *
 * @param suggester the suggester to bind as [DefaultSuggester]
 */
abstract class PlayerModule(private val suggester: Suggester?) : AbstractModule() {
    override fun configure() {
        bind(DefaultSuggester::class.java).toInstance(DefaultSuggester(suggester))
        bind(PlayerHistory::class.java).to(PlayerHistoryImpl::class.java).`in`(Scopes.SINGLETON)
    }
}
