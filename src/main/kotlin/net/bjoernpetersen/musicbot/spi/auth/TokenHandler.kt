package net.bjoernpetersen.musicbot.spi.auth

import net.bjoernpetersen.musicbot.api.auth.InvalidTokenException
import net.bjoernpetersen.musicbot.api.auth.Tokens
import net.bjoernpetersen.musicbot.api.auth.User

/**
 * Handles the creation and encoding of API tokens.
 */
interface TokenHandler {
    /**
     * Creates an access token for the user associated with the refresh token.
     * If the refresh token was about to expire, the result will also include a refresh token.
     *
     * @param refreshToken a refresh token
     * @return an access token and possibly a refresh token for that user
     * @throws InvalidTokenException if the refresh token was invalid
     */
    fun createTokens(refreshToken: String): Tokens

    /**
     * Creates an access token and a refresh token for the specified user.
     *
     * @param user a user
     * @return a token pair for that user
     */
    fun createTokens(user: User): Tokens

    /**
     * Create a user object from a JWT token.
     *
     * @param token the JWT token
     * @return a user object
     * @throws InvalidTokenException if the structure or signature of the token are invalid
     */
    @Throws(InvalidTokenException::class)
    fun decodeAccessToken(token: String): User

    /**
     * Create a refresh token for the specified user.
     *
     * @param user the user to create the token for
     * @return a refresh token
     */
    fun createRefreshToken(user: User): String

    /**
     * Invalidates all previous refresh tokens for the specified user.
     *
     * @param user any user
     */
    fun invalidateToken(user: User)
}
