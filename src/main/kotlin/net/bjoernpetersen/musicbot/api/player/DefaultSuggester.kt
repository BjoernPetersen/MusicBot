package net.bjoernpetersen.musicbot.api.player

import net.bjoernpetersen.musicbot.spi.plugin.Suggester

internal data class DefaultSuggester(val suggester: Suggester?)
