package net.bjoernpetersen.musicbot.api.auth

import org.mindrot.jbcrypt.BCrypt

sealed class User {
    abstract val name: String
    abstract val permissions: Set<Permission>

    abstract fun hasPassword(password: String): Boolean
}

data class GuestUser(
    override val name: String,
    val id: String) : User() {

    override fun hasPassword(password: String): Boolean = id == password
    override val permissions: Set<Permission> = emptySet()
}

data class FullUser(
    override val name: String,
    override val permissions: Set<Permission>,
    val hash: String) : User() {

    override fun hasPassword(password: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }
}

object BotUser : User() {
    override val name: String = "MusicBot"
    override val permissions: Set<Permission> = emptySet()
    override fun hasPassword(password: String): Boolean = false
}
