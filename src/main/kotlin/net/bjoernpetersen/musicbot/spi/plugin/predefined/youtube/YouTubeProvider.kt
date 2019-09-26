package net.bjoernpetersen.musicbot.spi.plugin.predefined.youtube

import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.plugin.Provider

/**
 * Provider for YouTube songs/videos.
 *
 * The song ID should be the YouTube video ID. For example "u4dT0NwJ5po".
 */
@IdBase("YouTube")
interface YouTubeProvider : Provider
