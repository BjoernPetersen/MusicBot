package net.bjoernpetersen.musicbot.spi.auth

import net.bjoernpetersen.musicbot.api.auth.BotUser
import net.bjoernpetersen.musicbot.api.auth.InvalidTokenException
import net.bjoernpetersen.musicbot.api.auth.User

/**
 * Handles the creation and encoding of API tokens.
 */
interface TokenHandler {
    /**
     * Creates a JWT token for the specified user.
     *
     * @param user a user
     * @return a JWT token for that user
     * @throws IllegalArgumentException if the user is the [BotUser]
     */
    fun createToken(user: User): String

    /**
     * Create a user object from a JWT token.
     *
     * @param token the JWT token
     * @return a user object
     * @throws InvalidTokenException if the structure or signature of the token are invalid
     */
    @Throws(InvalidTokenException::class)
    fun decodeToken(token: String): User

    /**
     * Invalidates all cached tokens for the specified user.
     *
     * @param user any user
     */
    fun invalidateToken(user: User)
}
