package net.bjoernpetersen.musicbot.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.SignatureVerificationException
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.config.ConfigManager
import net.bjoernpetersen.musicbot.api.config.GenericConfigScope
import net.bjoernpetersen.musicbot.spi.auth.UserDatabase
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TEMPORARY_USER_CAPACITY = 32
private const val TOKEN_TTL_MINUTES = 10L
private const val REFRESH_TOKEN_TTL_DAYS = 180L
private const val SIGNATURE_KEY_SIZE = 4096

/**
 * Manages all users (guest and full) and creates/verifies their tokens.
 */
@Singleton
@Suppress("TooManyFunctions")
class UserManager @Inject constructor(
    private val userDatabase: UserDatabase,
    configManager: ConfigManager
) {
    private val logger = KotlinLogging.logger { }

    private val secrets = configManager[GenericConfigScope(UserManager::class)].state
    private val signatureKey: Config.StringEntry = secrets.StringEntry("signatureKey", "", { null })
    private val guestSignatureKey: String = createSignatureKey()
    private val temporaryUsers: MutableMap<String, GuestUser> = HashMap(TEMPORARY_USER_CAPACITY)

    /**
     * Creates a temporary/guest user. This user is only valid for the duration of the current
     * bot lifetime.
     *
     * @param name the unique name of the new user
     * @param id an auto-generated password for the user (for example a UUID)
     * @return the generated user
     * @throws DuplicateUserException if a user with that name already exists
     */
    @Suppress("unused")
    @Throws(DuplicateUserException::class)
    fun createTemporaryUser(name: String, id: String): User {
        if (BotUser.name.equals(name, ignoreCase = true))
            throw DuplicateUserException("Invalid username")

        try {
            // TODO create "hasUser" method in Database
            getUser(name)
            throw DuplicateUserException("User already exists: $name")
        } catch (expected: UserNotFoundException) {
            val user = GuestUser(name, id, createSignatureKey())
            temporaryUsers[user.name.toId()] = user
            return user
        }
    }

    /**
     * Gets a user by the specified name.
     *
     * @param name the unique name identifying the user
     * @return the user with that name (possibly with different capitalization)
     * @throws UserNotFoundException if no such user exists
     */
    @Throws(UserNotFoundException::class)
    fun getUser(name: String): User {
        if (BotUser.name.equals(name, ignoreCase = true)) {
            return BotUser
        }
        return temporaryUsers[name.toId()]
            ?: userDatabase.findUser(name)
    }

    /**
     * Updates the specified user's password.
     *
     * If the user is a guest, this will make them a full user.
     *
     * @param user the user name
     * @param password the new password
     * @return a new, valid user object
     */
    @Suppress("unused")
    fun updateUser(user: User, password: String): FullUser {
        if (password.isEmpty()) {
            throw IllegalArgumentException()
        }

        val hash = hash(password)
        return FullUser(user.name, user.permissions, user.signature, hash).also {
            if (user is GuestUser) {
                try {
                    userDatabase.insertUser(it, hash)
                } catch (e: DuplicateUserException) {
                    throw IllegalStateException("Full and guest user with same name exist!", e)
                }
                temporaryUsers.remove(user.name.toId())
            } else {
                userDatabase.updatePassword(it.name, hash)
            }
        }
    }

    /**
     * Updates a user's permissions.
     *
     * @param user the user whose permissions should be updated
     * @param permissions the new set of permissions
     * @return a new user object with the new permissions
     * @throws IllegalArgumentException if the specified user is not a full user
     */
    @Suppress("unused")
    fun updatePermissions(user: User, permissions: Set<Permission>): FullUser {
        if (user !is FullUser) {
            throw IllegalArgumentException()
        }
        userDatabase.updatePermissions(user.name, permissions)
        return userDatabase.findUser(user.name)
    }

    /**
     * Permanently deletes a user.
     *
     * After calling this method, anyone is free to create a new user with that name.
     *
     * @param user the user to delete
     * @throws IllegalArgumentException if the specified user is the [BotUser]
     */
    @Suppress("unused")
    fun deleteUser(user: User) {
        when (user) {
            BotUser -> throw IllegalArgumentException("Can't delete BotUser")
            is FullUser -> userDatabase.deleteUser(user.name)
            is GuestUser -> temporaryUsers.remove(user.name.toId())
        }
    }

    /**
     * Creates a new signature for a user, invalidating all tokens singed with the old one.
     *
     * @param user the user to update
     * @return the new user with updated signature
     * @throws IllegalArgumentException if the specified user is the [BotUser]
     */
    @Suppress("unused")
    fun newUserSignature(user: User): User {
        return when (user) {
            BotUser -> throw IllegalArgumentException("Can't update signature for BotUser")
            is FullUser -> user.copy(signature = createSignatureKey())
                .also { userDatabase.updateSignature(it.name, it.signature) }
            is GuestUser -> user.copy(signature = createSignatureKey())
                .also { temporaryUsers[it.name.toId()] = it }
        }
    }

    /**
     * Gets all **full** users.
     *
     * The result **does not** contain any temporary users, because this method should be used
     * to display users whose permissions may be edited.
     *
     * @return a set of all users
     */
    @Suppress("unused")
    fun getUsers(): Set<FullUser> {
        return userDatabase.getUsers()
    }

    /**
     * Creates a JWT token for the specified user.
     *
     * @param user a user
     * @return a JWT token for that user
     * @throws IllegalArgumentException if the user is the [BotUser]
     */
    @Suppress("unused")
    fun toToken(user: User): String {
        return JWT.create()
            .withSubject(user.name)
            .withIssuedAt(Date())
            .withExpiresAt(Date.from(Instant.now().plus(Duration.ofMinutes(TOKEN_TTL_MINUTES))))
            .withArrayClaim("permissions", user.permissions.map { it.label }.toTypedArray())
            .sign(Algorithm.HMAC512(decideSignatureKey(user)))
    }

    /**
     * Creates a JWT token to refresh the access token generated by [toToken].
     *
     * @param user a [user]
     * @return a JWT refresh token for the [user]
     * @throws IllegalArgumentException if the [user] is the [BotUser]
     */
    @Suppress("unused")
    fun toRefreshToken(user: User): String {
        return JWT.create()
            .withSubject(user.name)
            .withIssuedAt(Date())
            .withExpiresAt(Date.from(Instant.now().plus(Duration.ofDays(REFRESH_TOKEN_TTL_DAYS))))
            .withClaim("refresh", true)
            .sign(Algorithm.HMAC512(user.signature));
    }

    /**
     * Decides which signing key is to be used.
     *
     */
    private fun decideSignatureKey(user: User): String {
        if (user is BotUser) throw IllegalArgumentException("Can't create a token for the bot user")
        return if (user is GuestUser) {
            guestSignatureKey
        } else {
            getSignatureKey()
        }
    }

    /**
     * Create a user object from a JWT token.
     *
     * @param token the JWT token
     * @return a user object
     * @throws InvalidTokenException if the structure or signature of the token are invalid
     */
    @Suppress("unused")
    @Throws(InvalidTokenException::class)
    fun fromToken(token: String): User {
        return try {
            decodeFullUserToken(token)
        } catch (e: SignatureVerificationException) {
            // try again with guest signature key
            decodeGuestUserToken(token)
        }
    }

    @Suppress("ThrowsCount")
    @Throws(InvalidTokenException::class, SignatureVerificationException::class)
    private fun decodeFullUserToken(token: String): FullUser {
        val decoded = try {
            JWT
                .require(Algorithm.HMAC512(getSignatureKey()))
                .build()
                .verify(token)
        } catch (e: SignatureVerificationException) {
            // This one should be propagated
            throw e
        } catch (e: JWTVerificationException) {
            throw InvalidTokenException(e)
        }
        val name = decoded.subject ?: throw InvalidTokenException("subject missing")
        val permissions: Set<Permission> = decoded
            .getClaim("permissions")
            .asList(String::class.java)
            .mapNotNull {
                try {
                    Permission.matchByLabel(it)
                } catch (e: IllegalArgumentException) {
                    logger.error { "Unknown permission in token: $it" }
                    null
                }
            }
            .toSet()

        return FullUser(name, permissions, "", "")
    }

    @Suppress("ThrowsCount")
    @Throws(InvalidTokenException::class)
    private fun decodeGuestUserToken(token: String): GuestUser {
        val decoded = try {
            JWT
                .require(Algorithm.HMAC512(guestSignatureKey))
                .build()
                .verify(token)
        } catch (e: JWTVerificationException) {
            throw InvalidTokenException(e)
        }

        val name = decoded.subject ?: throw InvalidTokenException("subject missing")
        return GuestUser(name, "", "")
    }

    private fun getSignatureKey(): String {
        return signatureKey.get() ?: createSignatureKey().also { signatureKey.set(it) }
    }

    /**
     * Decodes a jwt refresh-token
     *
     * @param token token to decode
     * @return The user associated with the token, if the token was valid and a refresh-token
     * @throws InvalidTokenException If the token was invalid or not a refresh-token
     * @throws UserNotFoundException If the user can not be found. This exception is to be expected for
     * [GuestUser]s after a restart of the server
     *
     */
    @Suppress("unused", "ThrowsCount")
    @Throws(InvalidTokenException::class, UserNotFoundException::class)
    fun decodeRefreshToken(token: String): User {
        val decoded = try {
            JWT.decode(token)
        } catch (e: JWTDecodeException) {
            throw InvalidTokenException(e)
        }
        val name = decoded.subject ?: throw InvalidTokenException("subject missing")
        val refreshClaim = decoded.getClaim("refresh")
        if (refreshClaim.isNull || !refreshClaim.asBoolean()) {
            throw InvalidTokenException("not a refresh token")
        }
        val user = getUser(name)
        try {
            JWT
                .require((Algorithm.HMAC512(user.signature)))
                .build()
                .verify(token)
        } catch (e: JWTVerificationException) {
            throw InvalidTokenException(e)
        }
        return user;
    }
}

private fun hash(password: String): String {
    return BCrypt.hashpw(password, BCrypt.gensalt())
}

/**
 * craete a random utf-8 string of length [SIGNATURE_KEY_SIZE] that can be used as signature
 */
fun createSignatureKey(): String {
    val rand = SecureRandom()
    val bytes = ByteArray(SIGNATURE_KEY_SIZE)
    rand.nextBytes(bytes)
    return String(bytes, Charsets.UTF_8)
}
