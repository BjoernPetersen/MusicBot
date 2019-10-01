package net.bjoernpetersen.musicbot.spi.plugin.predefined.spotify

import net.bjoernpetersen.musicbot.api.plugin.Base
import net.bjoernpetersen.musicbot.spi.plugin.predefined.Authenticator

/**
 * Authenticator providing a Spotify Web API token.
 */
@Base
interface SpotifyAuthenticator : Authenticator {
    /**
     * Require the specified access token scopes.
     */
    fun requireScopes(vararg scopes: SpotifyScope)
}
