package net.bjoernpetersen.musicbot.internal.player

import net.bjoernpetersen.musicbot.api.player.PlayState
import net.bjoernpetersen.musicbot.api.player.SongEntry
import net.bjoernpetersen.musicbot.spi.player.Player
import net.bjoernpetersen.musicbot.spi.player.PlayerHistory
import java.util.LinkedList
import javax.inject.Inject

internal class PlayerHistoryImpl @Inject private constructor(player: Player) : PlayerHistory {

    private val history = LinkedList<SongEntry>()

    init {
        player.addListener { _, new ->
            if (new is PlayState) {
                val entry = new.entry
                if (history.lastOrNull() != entry) {
                    if (history.size == MAX_SIZE) history.poll()
                    history.add(entry)
                }
            }
        }
    }

    override fun getHistory(limit: Int): List<SongEntry> {
        return if (history.size > limit) history.subList(history.size - limit, history.size)
        else history
    }

    private companion object {
        const val MAX_SIZE = 40
    }
}
