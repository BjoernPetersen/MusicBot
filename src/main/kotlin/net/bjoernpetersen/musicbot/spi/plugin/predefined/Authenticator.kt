package net.bjoernpetersen.musicbot.spi.plugin.predefined

import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin

/**
 * Authenticator for a service.
 *
 * An authenticator provides an access token for a service.
 * The specific service is defined by extending interfaces.
 *
 * This interface doesn't need to be used, but it's recommended.
 */
interface Authenticator : GenericPlugin {
    /**
     * Gets a token, either from cache or requesting a new one.
     *
     * This method should be called every time a token is needed.
     * Callers should not store the returned value anywhere.
     *
     * @return a valid (not expired) token
     * @throws TokenRefreshException if no valid token was cached and no new one could be obtained
     */
    @Throws(TokenRefreshException::class)
    suspend fun getToken(): String

    /**
     * Invalidates the cached token (if any) without retrieving a new one.
     * This method may not perform any IO operations.
     */
    fun invalidateToken()
}
