package com.github.bjoernpetersen.musicbot.api.auth

import com.github.bjoernpetersen.musicbot.api.config.Config
import com.github.bjoernpetersen.musicbot.api.config.ConfigManager
import com.github.bjoernpetersen.musicbot.api.config.GenericConfigScope
import com.github.bjoernpetersen.musicbot.spi.auth.DuplicateUserException
import com.github.bjoernpetersen.musicbot.spi.auth.UserDatabase
import com.github.bjoernpetersen.musicbot.spi.auth.UserNotFoundException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.crypto.MacSigner
import org.mindrot.jbcrypt.BCrypt
import java.nio.charset.StandardCharsets
import java.sql.SQLException
import java.time.Instant
import java.time.Period
import java.util.*
import javax.inject.Inject

class UserManager @Inject constructor(
    private val userDatabase: UserDatabase,
    configManager: ConfigManager) {

    private val secrets = configManager[GenericConfigScope(UserManager::class)].state
    private val signatureKey: Config.StringEntry = secrets.StringEntry("signatureKey", "", { null })
    private val guestSignatureKey: String = createSignatureKey()
    private val temporaryUsers: MutableMap<String, GuestUser> = HashMap(32)


    @Throws(DuplicateUserException::class)
    fun createTemporaryUser(name: String, id: String): User {
        if (BotUser.name.equals(name, ignoreCase = true))
            throw DuplicateUserException("Invalid username")
        if (BotUser.id == id) throw DuplicateUserException("Invalid ID")

        try {
            // TODO create "hasUser" method in Database
            getUser(name)
            throw DuplicateUserException("User already exists: $name")
        } catch (expected: UserNotFoundException) {
            val user = GuestUser(name, id)
            temporaryUsers[user.id] = user
            return user
        }

    }

    @Throws(UserNotFoundException::class)
    fun getUser(id: String): User {
        if (BotUser.id == id) {
            return BotUser
        }
        return temporaryUsers[id]
            ?: userDatabase.findUser(id)
            ?: throw UserNotFoundException("Could not find user: $id")
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
        return FullUser(user.name, user.id, user.permissions, hash).also {
            if (user is GuestUser) {
                userDatabase.insertUser(it)
                temporaryUsers.remove(user.id)
            } else {
                userDatabase.updatePassword(it)
            }
        }
    }

    @Throws(SQLException::class)
    fun updateUser(user: User, permissions: Set<Permission>): FullUser {
        if (user is GuestUser) {
            throw IllegalArgumentException()
        }
        userDatabase.updatePermissions(user.id, permissions)
        return userDatabase.findUser(user.id) ?: throw SQLException()
    }

    @Throws(SQLException::class)
    fun deleteUser(user: FullUser) {
        userDatabase.deleteUser(user.id)
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
        val builder = Jwts.builder()
            .setSubject(user.id)
            .setIssuedAt(Date())
            .signWith(SignatureAlgorithm.HS512, signatureKey)
            .setExpiration(Date.from(Instant.now().plus(Period.ofDays(7))))

        for (permission in user.permissions) {
            builder.claim(permission.label, true)
        }

        return builder.compact()
    }

    @Throws(InvalidTokenException::class)
    fun fromToken(token: String): User {
        val parsed: Jws<Claims> = try {
            Jwts.parser()
                .setSigningKey(getSignatureKey())
                .parseClaimsJws(token)
        } catch (e: JwtException) {
            // try again with guest key
            try {
                Jwts.parser()
                    .setSigningKey(guestSignatureKey)
                    .parseClaimsJws(token)
            } catch (e1: JwtException) {
                e1.addSuppressed(e)
                throw InvalidTokenException(e1)
            }
        }

        val id = parsed.body.subject ?: throw InvalidTokenException("ID missing")

        try {
            return getUser(id)
        } catch (e: UserNotFoundException) {
            throw InvalidTokenException(e)
        }
    }

    private fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    private fun getSignatureKey(): String {
        return signatureKey.get() ?: createSignatureKey().also { signatureKey.set(it) }
    }

    private fun createSignatureKey(): String {
        val encoded = Base64.getEncoder().encode(MacSigner.generateKey().encoded)
        return String(encoded, StandardCharsets.UTF_8)
    }

}
