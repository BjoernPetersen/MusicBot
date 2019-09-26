package net.bjoernpetersen.musicbot.spi.plugin.predefined.gplaymusic

import net.bjoernpetersen.musicbot.api.plugin.Base
import net.bjoernpetersen.musicbot.spi.plugin.predefined.Authenticator

/**
 * Authenticator providing a Google Play Music API token.
 */
@Base
interface GPlayMusicAuthenticator : Authenticator
