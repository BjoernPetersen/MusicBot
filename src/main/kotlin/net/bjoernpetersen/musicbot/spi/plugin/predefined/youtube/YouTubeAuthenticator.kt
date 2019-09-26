package net.bjoernpetersen.musicbot.spi.plugin.predefined.youtube

import net.bjoernpetersen.musicbot.api.plugin.Base
import net.bjoernpetersen.musicbot.spi.plugin.predefined.Authenticator

/**
 * Authenticator providing a YouTube API key.
 */
@Base
interface YouTubeAuthenticator : Authenticator
