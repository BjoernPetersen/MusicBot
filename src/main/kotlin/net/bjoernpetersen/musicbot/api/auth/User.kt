package net.bjoernpetersen.musicbot.api.auth

import net.bjoernpetersen.musicbot.api.auth.BotUser.hasPassword
import net.bjoernpetersen.musicbot.api.player.QueueEntry
import org.mindrot.jbcrypt.BCrypt
import java.util.Locale

/**
 * Represents a user of the MusicBot.
 *
 * This is a sealed class with exactly three implementations:
 *
 * - [GuestUser]
 * - [FullUser]
 * - [BotUser]
 *
 * ## Identification
 *
 * Users are identified by their [name]. Names are required to be unique across all users.
 *
 * Before comparing names, they are converted to lower-case using the [US locale][Locale.US].
 * This also means there can't be two users using the same name with different capitalization.
 *
 * ## Password
 *
 * Neither the password nor the password hash of a user is directly accessible.
 * To test if a given password is correct, the [hasPassword] method must be used.
 */
sealed class User {
    /**
     * The name of the user. May be composed of any unicode characters.
     */
    abstract val name: String

    /**
     * A set of permissions this user has.
     */
    abstract val permissions: Set<Permission>

    /**
     * A function to test whether this user has the specified password.
     *
     * @param password the plain-text password
     * @return whether the password is correct
     */
    abstract fun hasPassword(password: String): Boolean
}

/**
 * Guest users are temporary users who are only held in memory during one bot session.
 * When the bot is closed, all guest users are lost.
 *
 * All new users are initially guest users, who can then be upgraded to [full users][FullUser] by
 * providing a real password.
 *
 * ## Authentication
 *
 * Guests don't need to provide a password in order to lower the barrier for bot usage.
 * In lieu of a password, clients send some unique [identifier][id] like an UUID or an installation
 * ID which is then used the same way passwords are for [full users][FullUser].
 *
 * Optimally, the [id] should be reproducible by the client and **only** the one client it came from.
 *
 * ## Limitations
 *
 * Due to the questionable security of guest "passwords", all guest users have exactly the
 * permissions defined by [DefaultPermissions].
 *
 * @param name a name which identifies the user
 * @param id some unique identifier (see `Authentication`)
 */
data class GuestUser(
    override val name: String,
    private val id: String
) : User() {
    override fun hasPassword(password: String): Boolean {
        check(!id.isBlank())
        return id == password
    }

    override val permissions: Set<Permission> = DefaultPermissions.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GuestUser) return false

        if (name.toId() != other.name.toId()) return false

        return true
    }

    override fun hashCode(): Int {
        return name.toId().hashCode()
    }
}

/**
 * A registered, permanent, full user.
 *
 * Note that [hash] may be empty if the user is constructed from a token.
 *
 * @param name a name which identifies the user
 * @param permissions the permissions of the user
 * @param hash the password hash of the user, or an empty string
 */
data class FullUser(
    override val name: String,
    override val permissions: Set<Permission>,
    private val hash: String
) : User() {

    override fun hasPassword(password: String): Boolean {
        check(!hash.isBlank())
        return BCrypt.checkpw(password, hash)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FullUser) return false

        if (name.toId() != other.name.toId()) return false

        return true
    }

    override fun hashCode(): Int {
        return name.toId().hashCode()
    }
}

/**
 * A special user which can be used for automated actions.
 *
 * For example, if a [QueueEntry] is created by a plugin and no user is available
 * to associate it with, this user may be used.
 *
 * This user is only intended for automatic actions, it doesn't have any permissions and
 * its [hasPassword] method doesn't match anything.
 */
object BotUser : User() {
    override val name: String = "MusicBot"
    override val permissions: Set<Permission> = emptySet()
    override fun hasPassword(password: String): Boolean = false
}

/**
 * Creates an ID from a user's name.
 */
fun String.toId(): String = trim().toLowerCase(Locale.US)
