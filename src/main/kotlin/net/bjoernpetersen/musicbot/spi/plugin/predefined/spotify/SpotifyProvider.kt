package net.bjoernpetersen.musicbot.spi.plugin.predefined.spotify

import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.plugin.Provider

/**
 * Provider for Spotify songs.
 *
 * Song IDs should be the Spotify track ID.
 */
@IdBase("Spotify")
interface SpotifyProvider : Provider
