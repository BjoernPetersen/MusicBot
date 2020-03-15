package net.bjoernpetersen.musicbot.internal.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.inject.Inject
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.auth.Crypto
import net.bjoernpetersen.musicbot.api.auth.FullUser
import net.bjoernpetersen.musicbot.api.auth.GuestUser
import net.bjoernpetersen.musicbot.api.auth.InvalidSignatureException
import net.bjoernpetersen.musicbot.api.auth.InvalidTokenException
import net.bjoernpetersen.musicbot.api.auth.Permission
import net.bjoernpetersen.musicbot.api.auth.Tokens
import net.bjoernpetersen.musicbot.api.auth.User
import net.bjoernpetersen.musicbot.api.auth.UserManager
import net.bjoernpetersen.musicbot.api.auth.toId
import net.bjoernpetersen.musicbot.api.config.ByteArraySerializer
import net.bjoernpetersen.musicbot.api.config.ConfigManager
import net.bjoernpetersen.musicbot.api.config.GenericConfigScope
import net.bjoernpetersen.musicbot.api.config.serialized
import net.bjoernpetersen.musicbot.spi.auth.TokenHandler

private const val ACCESS_TOKEN_TTL_MINUTES = 10L
private const val REFRESH_TOKEN_TTL_MONTHS = 6L
private const val REFRESH_TOKEN_LIMIT_DAYS = 14L

@Suppress("TooManyFunctions")
internal class DefaultTokenHandler @Inject private constructor(
    configManager: ConfigManager,
    refreshClaimDatabaseImpl: RefreshClaimDatabaseImpl,
    private val userManager: UserManager
) : TokenHandler {
    private val logger = KotlinLogging.logger { }

    private val refreshClaimDatabase = CachedRefreshClaimDatabase(refreshClaimDatabaseImpl)

    private val secrets = configManager[GenericConfigScope(DefaultTokenHandler::class)].state
    private val signatureKey by secrets.serialized<ByteArray> {
        description = ""
        serializer = ByteArraySerializer
        check { null }
    }
    private val guestSignatureKey: ByteArray = Crypto.createRandomBytes()

    override fun createTokens(refreshToken: String): Tokens {
        val token = decodeToken(refreshToken)
        val user = userManager.getUser(token.jwt.subject)
        val accessToken = createAccessToken(user)

        val expiration = token.jwt.expiresAt.toInstant()
        val twoWeeksFromNow = Instant.now().plus(Duration.ofDays(REFRESH_TOKEN_LIMIT_DAYS))
        return if (expiration.isBefore(twoWeeksFromNow)) {
            Tokens(accessToken, createRefreshToken(user))
        } else {
            Tokens(accessToken, null)
        }
    }

    override fun createTokens(user: User): Tokens {
        return Tokens(createAccessToken(user), createRefreshToken(user))
    }

    override fun createRefreshToken(user: User): String {
        return JWT.create()
            .withSubject(user.name)
            .withIssuedAt(Date())
            .withExpiresAt(
                Date.from(
                    Instant.now().plus(Duration.of(REFRESH_TOKEN_TTL_MONTHS, ChronoUnit.MONTHS))
                )
            )
            .sign(Algorithm.HMAC512(getSignatureKey(user)))
    }

    private fun createAccessToken(user: User): String {
        return JWT.create()
            .withSubject(user.name)
            .withIssuedAt(Date())
            .withExpiresAt(
                Date.from(
                    Instant.now().plus(Duration.ofMinutes(ACCESS_TOKEN_TTL_MINUTES))
                )
            )
            .withArrayClaim("permissions", user.permissions.map { it.label }.toTypedArray())
            .sign(Algorithm.HMAC512(getSignatureKey(user)))
    }

    override fun decodeAccessToken(token: String): User {
        return getUserFromToken(decodeToken(token))
    }

    override fun invalidateToken(user: User) {
        refreshClaimDatabase.invalidateClaim(user.name.toId())
    }

    private fun decodeToken(token: String): DecodedToken {
        return try {
            val decoded = decodeToken(token, getSignatureKey())
            DecodedToken(decoded, false)
        } catch (e: InvalidSignatureException) {
            // try again with guest signature key
            val decoded = decodeToken(token, guestSignatureKey)
            DecodedToken(decoded, true)
        }
    }

    @Throws(InvalidTokenException::class)
    private fun decodeToken(token: String, signatureKey: ByteArray): DecodedJWT {
        return try {
            JWT
                .require(Algorithm.HMAC512(signatureKey))
                .build()
                .verify(token)
        } catch (e: SignatureVerificationException) {
            // This one should be propagated
            throw InvalidSignatureException()
        } catch (e: JWTVerificationException) {
            throw InvalidTokenException(e)
        }
    }

    private fun getUserFromToken(token: DecodedToken): User {
        val jwt = token.jwt
        val name = jwt.subject!!
        val permissionClaim = jwt.getClaim("permissions")
        return if (token.isGuest) {
            GuestUser(name, "")
        } else {
            val permissions: Set<Permission> = permissionClaim.asList(String::class.java)
                .mapNotNull {
                    try {
                        Permission.matchByLabel(it)
                    } catch (e: IllegalArgumentException) {
                        logger.error { "Unknown permission in token: $it" }
                        null
                    }
                }
                .toSet()
            FullUser(name, permissions, "")
        }
    }

    private fun getSignatureKey(user: User): ByteArray {
        return if (user is GuestUser) {
            this.guestSignatureKey
        } else {
            getSignatureKey()
        }
    }

    private fun getSignatureKey(): ByteArray {
        return signatureKey.get() ?: Crypto.createRandomBytes().also { signatureKey.set(it) }
    }
}

private data class DecodedToken(val jwt: DecodedJWT, val isGuest: Boolean)
