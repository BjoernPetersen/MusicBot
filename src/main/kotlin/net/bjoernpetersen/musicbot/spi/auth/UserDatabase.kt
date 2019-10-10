package net.bjoernpetersen.musicbot.spi.auth

import net.bjoernpetersen.musicbot.api.auth.DuplicateUserException
import net.bjoernpetersen.musicbot.api.auth.FullUser
import net.bjoernpetersen.musicbot.api.auth.Permission
import net.bjoernpetersen.musicbot.api.auth.UserNotFoundException

/**
 * Manages all full users in a database.
 */
interface UserDatabase {
    /**
     * Finds a specific user by his username.
     *
     * The name will be trimmed and converted to lower-case according to the US locale before
     * checking uniqueness.
     *
     * @param name the user's name
     * @return the user object for that name
     * @throws UserNotFoundException if no user with that name exists
     */
    @Throws(UserNotFoundException::class)
    fun findUser(name: String): FullUser

    /**
     * Gets a list of all registered users.
     */
    fun getUsers(): Set<FullUser>

    /**
     * Inserts a new user into the database.
     *
     * @param user a user to insert into the database
     * @param hash the user's hashed password
     * @throws DuplicateUserException if a full user with an equal name already exists
     */
    @Throws(DuplicateUserException::class)
    fun insertUser(user: FullUser, hash: String)

    /**
     * Updates the password of a user.
     *
     * The name will be trimmed and converted to lower-case according to the US locale before
     * checking uniqueness.
     *
     * @param name the name of the user whose password should be updated
     * @param hash the user's hashed password
     * @throws UserNotFoundException if there is no such user
     */
    fun updatePassword(name: String, hash: String)

    /**
     * Updates the permissions of a user.
     *
     * The name will be trimmed and converted to lower-case according to the US locale before
     * checking uniqueness.
     *
     * @param name the user who's password should be updated
     * @param permissions the full list of the user's new permissions
     * @throws UserNotFoundException if there is no such user
     */
    fun updatePermissions(name: String, permissions: Set<Permission>)

    /**
     * Updates the signature of a user
     *
     * The name will be trimmed and converted to lower-case according to the US locale before
     * checking uniqueness.
     *
     * @param name the user who's password should be updated
     * @param signature the new signature of the user
     * @throws UserNotFoundException if there is no such user
     */
    fun updateSignature(name: String, signature: String)

    /**
     * Deletes a user from the database.
     *
     * The name will be trimmed and converted to lower-case according to the US locale before
     * checking uniqueness.
     *
     * @param name the user's name
     */
    fun deleteUser(name: String)

    /**
     * Releases all resources and renders this object unusable.
     */
    fun close()
}
