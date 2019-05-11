package net.bjoernpetersen.musicbot.api.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.config.ConfigManager
import net.bjoernpetersen.musicbot.api.config.GenericConfigScope
import net.bjoernpetersen.musicbot.spi.auth.UserDatabase
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.sql.SQLException
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.HashMap
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManager @Inject constructor(
    private val userDatabase: UserDatabase,
    configManager: ConfigManager
) {

    private val logger = KotlinLogging.logger { }

    private val secrets = configManager[GenericConfigScope(UserManager::class)].state
    private val signatureKey: Config.StringEntry = secrets.StringEntry("signatureKey", "", { null })
    private val guestSignatureKey: String = createSignatureKey()
    private val temporaryUsers: MutableMap<String, GuestUser> = HashMap(32)


    @Throws(DuplicateUserException::class)
    fun createTemporaryUser(name: String, id: String): User {
        if (BotUser.name.equals(name, ignoreCase = true))
            throw DuplicateUserException("Invalid username")

        try {
            // TODO create "hasUser" method in Database
            getUser(name)
            throw DuplicateUserException("User already exists: $name")
        } catch (expected: UserNotFoundException) {
            val user = GuestUser(name, id)
            temporaryUsers[user.name.toLowerCase(Locale.US)] = user
            return user
        }
    }

    @Throws(UserNotFoundException::class)
    fun getUser(name: String): User {
        if (BotUser.name == name) {
            return BotUser
        }
        return temporaryUsers[name.toLowerCase(Locale.US)]
            ?: userDatabase.findUser(name)
            ?: throw UserNotFoundException(
                "Could not find user: $name"
            )
    }

    /**
     * Updates the specified users password.
     *
     * If the user is a guest, this will make them a full user.
     *
     * @param user the user name
     * @param password the new password
     * @return a new, valid user object
     * @throws DuplicateUserException if the specified user was a guest and a full user with the same
     * name already existed
     * @throws SQLException if any SQL errors occur
     */
    @Throws(DuplicateUserException::class, SQLException::class)
    fun updateUser(user: User, password: String): FullUser {
        if (password.isEmpty()) {
            throw IllegalArgumentException()
        }

        val hash = hash(password)
        return FullUser(user.name, user.permissions, hash).also {
            if (user is GuestUser) {
                userDatabase.insertUser(it, hash)
                temporaryUsers.remove(user.name.toLowerCase(Locale.US))
            } else {
                userDatabase.updatePassword(it, hash)
            }
        }
    }

    @Throws(SQLException::class)
    fun updateUser(user: User, permissions: Set<Permission>): FullUser {
        if (user is GuestUser) {
            throw IllegalArgumentException()
        }
        userDatabase.updatePermissions(user.name, permissions)
        return userDatabase.findUser(user.name) ?: throw SQLException()
    }

    @Throws(SQLException::class)
    fun deleteUser(user: User) {
        if (user is FullUser) userDatabase.deleteUser(user.name)
        else temporaryUsers.remove(user.name)
    }

    /**
     * Gets all full users.
     *
     * @return a list of users
     * @throws SQLException if any SQL errors occur
     */
    @Throws(SQLException::class)
    fun getUsers(): Set<FullUser> {
        return userDatabase.getUsers()
    }

    fun toToken(user: User): String {
        val signatureKey: String = if (user is GuestUser) {
            this.guestSignatureKey
        } else {
            getSignatureKey()
        }

        return JWT.create()
            .withSubject(user.name)
            .withIssuedAt(Date())
            .withExpiresAt(Date.from(Instant.now().plus(Duration.ofMinutes(10))))
            .withArrayClaim("permissions", user.permissions.map { it.label }.toTypedArray())
            .sign(Algorithm.HMAC512(signatureKey))
    }

    @Throws(InvalidTokenException::class)
    fun fromToken(token: String): User {
        try {
            val decoded = JWT
                .require(Algorithm.HMAC512(getSignatureKey()))
                .build()
                .verify(token)
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

            return FullUser(name, permissions, "")
        } catch (e: JWTVerificationException) {
            // try again with guest key
            val decoded = try {
                JWT
                    .require(Algorithm.HMAC512(guestSignatureKey))
                    .build()
                    .verify(token)
            } catch (e1: JWTVerificationException) {
                e1.addSuppressed(e)
                throw InvalidTokenException(e1)
            }

            val name = decoded.subject ?: throw InvalidTokenException("subject missing")
            return GuestUser(name, "");
        }
    }

    private fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    private fun getSignatureKey(): String {
        return signatureKey.get() ?: createSignatureKey().also { signatureKey.set(it) }
    }

    private fun createSignatureKey(): String {
        val rand = SecureRandom()
        val bytes = ByteArray(4096)
        rand.nextBytes(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}
