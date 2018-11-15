package com.github.bjoernpetersen.musicbot.api.auth

import org.mindrot.jbcrypt.BCrypt
import java.util.*

sealed class User {
    abstract val name: String
    abstract val id: String
    abstract val permissions: Set<Permission>
}

data class GuestUser(override val name: String, override val id: String) : User() {
    override val permissions: Set<Permission> = emptySet()
}

data class FullUser(
    override val name: String,
    override val id: String,
    override val permissions: Set<Permission>,
    val hash: String) : User() {

    fun hasPassword(password: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }
}

object BotUser : User() {
    override val name: String = "MusicBot"
    override val id: String = UUID.randomUUID().toString()
    override val permissions: Set<Permission> = emptySet()
}
