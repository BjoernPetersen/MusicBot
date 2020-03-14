package net.bjoernpetersen.musicbot.internal.auth

import java.util.HashMap
import net.bjoernpetersen.musicbot.api.auth.DuplicateUserException
import net.bjoernpetersen.musicbot.api.auth.GuestUser
import net.bjoernpetersen.musicbot.api.auth.User
import net.bjoernpetersen.musicbot.api.auth.toId

private const val TEMPORARY_USER_CAPACITY = 32

internal class TempUserDatabase {
    private val temporaryUsers: MutableMap<String, GuestUser> = HashMap(TEMPORARY_USER_CAPACITY)

    /**
     * Finds a specific user by his username.
     *
     * The name will be trimmed and converted to lower-case according to the US locale before
     * checking uniqueness.
     *
     * @param name the user's name
     * @return the user object for that name
     */
    fun findUser(name: String): User? {
        return temporaryUsers[name.toId()]
    }

    /**
     * Inserts a new user into the database.
     *
     * @param name the name of a user to insert into the database
     * @param id the user's password-like ID
     * @throws DuplicateUserException if a full user with an equal name already exists
     */
    @Throws(DuplicateUserException::class)
    fun insertUser(name: String, id: String): User {
        val user = GuestUser(name, id)
        val prev = temporaryUsers.putIfAbsent(name.toId(), user)
        if (prev != null) throw DuplicateUserException("Temp user already exists: $name")
        return user
    }

    /**
     * Deletes a user from the database.
     *
     * The name will be trimmed and converted to lower-case according to the US locale before
     * checking uniqueness.
     *
     * @param name the user's name
     */
    fun deleteUser(name: String) {
        temporaryUsers.remove(name.toId())
    }
}
