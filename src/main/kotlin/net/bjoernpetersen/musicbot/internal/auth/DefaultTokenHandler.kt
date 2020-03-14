package net.bjoernpetersen.musicbot.internal.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.SignatureVerificationException
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.auth.BotUser
import net.bjoernpetersen.musicbot.api.auth.Crypto
import net.bjoernpetersen.musicbot.api.auth.FullUser
import net.bjoernpetersen.musicbot.api.auth.GuestUser
import net.bjoernpetersen.musicbot.api.auth.InvalidSignatureException
import net.bjoernpetersen.musicbot.api.auth.InvalidTokenException
import net.bjoernpetersen.musicbot.api.auth.Permission
import net.bjoernpetersen.musicbot.api.auth.User
import net.bjoernpetersen.musicbot.api.config.ByteArraySerializer
import net.bjoernpetersen.musicbot.api.config.ConfigManager
import net.bjoernpetersen.musicbot.api.config.GenericConfigScope
import net.bjoernpetersen.musicbot.api.config.serialized
import net.bjoernpetersen.musicbot.spi.auth.TokenHandler

private const val ACCESS_TOKEN_TTL_MINUTES = 10L

// TODO create module allowing access to this
internal class DefaultTokenHandler @Inject private constructor(
    configManager: ConfigManager
) : TokenHandler {
    private val logger = KotlinLogging.logger { }

    private val secrets = configManager[GenericConfigScope(DefaultTokenHandler::class)].state
    private val signatureKey by secrets.serialized<ByteArray> {
        description = ""
        serializer = ByteArraySerializer
        check { null }
    }
    private val guestSignatureKey: ByteArray = Crypto.createRandomBytes()

    override fun createToken(user: User): String {
        if (user is BotUser) throw IllegalArgumentException("Can't create a token for the bot user")
        val signatureKey: ByteArray = if (user is GuestUser) {
            this.guestSignatureKey
        } else {
            getSignatureKey()
        }

        return JWT.create()
            .withSubject(user.name)
            .withIssuedAt(Date())
            .withExpiresAt(
                Date.from(
                    Instant.now().plus(Duration.ofMinutes(ACCESS_TOKEN_TTL_MINUTES))
                )
            )
            .withArrayClaim("permissions", user.permissions.map { it.label }.toTypedArray())
            .sign(Algorithm.HMAC512(signatureKey))
    }

    override fun decodeToken(token: String): User {
        return try {
            decodeToken(token, getSignatureKey())
        } catch (e: InvalidSignatureException) {
            // try again with guest signature key
            decodeToken(token, guestSignatureKey)
        }
    }

    override fun invalidateToken(user: User) {
        // TODO: not possible right now
    }

    @Throws(InvalidTokenException::class)
    private fun decodeToken(token: String, signatureKey: ByteArray): User {
        val decoded = try {
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
        val name = decoded.subject!!
        val permissionClaim = decoded.getClaim("permissions")
        return if (permissionClaim.isNull) {
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

    private fun getSignatureKey(): ByteArray {
        return signatureKey.get() ?: Crypto.createRandomBytes().also { signatureKey.set(it) }
    }
}
