package net.bjoernpetersen.musicbot.api.auth

import mu.KotlinLogging
import net.bjoernpetersen.musicbot.internal.auth.TempUserDatabase
import net.bjoernpetersen.musicbot.spi.auth.UserDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all users (guest and full) and creates/verifies their tokens.
 */
@Singleton
@Suppress("TooManyFunctions")
class UserManager @Inject private constructor(
    private val userDatabase: UserDatabase,
    private val tempUserDatabase: TempUserDatabase
) {
    private val logger = KotlinLogging.logger { }

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
            return tempUserDatabase.insertUser(name, id)
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

        return tempUserDatabase.findUser(name) ?: userDatabase.findUser(name)
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

        val hash = Crypto.hash(password)
        return FullUser(user.name, user.permissions, hash).also {
            if (user is GuestUser) {
                try {
                    userDatabase.insertUser(it, hash)
                } catch (e: DuplicateUserException) {
                    throw IllegalStateException("Full and guest user with same name exist!", e)
                }
                tempUserDatabase.deleteUser(user.name)
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
            is GuestUser -> tempUserDatabase.deleteUser(user.name)
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
}
