package net.bjoernpetersen.musicbot.api.auth

/**
 * A pair of tokens.
 *
 * @param accessToken a JWT access token
 * @param refreshToken a refresh token, or null
 */
data class Tokens(val accessToken: String, val refreshToken: String? = null)
