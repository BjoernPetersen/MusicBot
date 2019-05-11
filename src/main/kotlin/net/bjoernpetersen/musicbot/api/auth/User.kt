package net.bjoernpetersen.musicbot.api.auth

import org.mindrot.jbcrypt.BCrypt

sealed class User {
    abstract val name: String
    abstract val permissions: Set<Permission>

    abstract fun hasPassword(password: String): Boolean
}

data class GuestUser(
    override val name: String,
    val id: String
) : User() {

    override fun hasPassword(password: String): Boolean {
        if (id.isBlank()) throw IllegalStateException()
        return id == password
    }

    override val permissions: Set<Permission> = DefaultPermissions.defaultPermissions

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GuestUser) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

/**
 * A registered, full user.
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
        if (hash.isBlank()) {
            throw IllegalStateException()
        }
        return BCrypt.checkpw(password, hash)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GuestUser) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

object BotUser : User() {
    override val name: String = "MusicBot"
    override val permissions: Set<Permission> = emptySet()
    override fun hasPassword(password: String): Boolean = false
}
