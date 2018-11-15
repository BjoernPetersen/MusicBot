package com.github.bjoernpetersen.musicbot.internal.auth

import com.github.bjoernpetersen.musicbot.api.auth.FullUser
import com.github.bjoernpetersen.musicbot.api.auth.Permission
import com.github.bjoernpetersen.musicbot.spi.auth.DuplicateUserException
import com.github.bjoernpetersen.musicbot.spi.auth.UserDatabase
import com.github.bjoernpetersen.musicbot.spi.auth.UserNotFoundException
import com.google.common.base.Splitter
import java.sql.DriverManager
import java.sql.SQLException

internal class DefaultDatabase(databaseUrl: String) : UserDatabase {
    private val connection = DriverManager.getConnection(databaseUrl)

    private val getUser = connection.prepareStatement(
        "SELECT name, password, permissions FROM users WHERE id=?")
    private val getUsers = connection.prepareStatement("SELECT * FROM users")
    private val createUser = connection.prepareStatement(
        "INSERT OR ABORT INTO users(id, name, password, permissions) VALUES(?, ?, ?, ?)")
    private val updatePassword = connection.prepareStatement(
        "UPDATE users SET password=? WHERE id=?")
    private val updatePermissions = connection.prepareStatement(
        "UPDATE users SET permissions=? WHERE id=?")
    private val deleteUser = connection.prepareStatement("DELETE FROM users WHERE id=?")

    init {
        connection.createStatement().use { statement ->
            statement.execute(
                """CREATE TABLE IF NOT EXISTS users(
                    id TEXT PRIMARY KEY UNIQUE NOT NULL,
                    name TEXT NOT NULL,
                    password TEXT NOT NULL,
                    permissions TEXT NOT NULL)""".trimMargin())
        }
    }

    private fun getPermissions(permissionString: String): Set<Permission> = Splitter.on(',')
        .omitEmptyStrings()
        .split(permissionString)
        .mapTo(HashSet()) { Permission.matchByLabel(it) }

    override fun findUser(id: String): FullUser? {
        synchronized(getUser) {
            try {
                getUser.clearParameters()
                getUser.setString(1, id)
                var name: String
                var hash: String
                var permissionString: String
                getUser.executeQuery().use { resultSet ->
                    if (!resultSet.next()) {
                        throw UserNotFoundException("No such user: $id")
                    }
                    name = resultSet.getString("name")
                    hash = resultSet.getString("password")
                    permissionString = resultSet.getString("permissions")
                    val permissions = getPermissions(permissionString)
                    return FullUser(name, id, permissions, hash)
                }
            } catch (e: SQLException) {
                throw UserNotFoundException(e)
            }
        }
    }

    override fun getUsers(): Set<FullUser> {
        synchronized(getUsers) {
            getUsers.executeQuery().use { resultSet ->
                val users = HashSet<FullUser>()
                while (resultSet.next()) {
                    val id = resultSet.getString("id")
                    val name = resultSet.getString("name")
                    val hash = resultSet.getString("password")
                    val permissionString = resultSet.getString("permissions")
                    val permissions = getPermissions(permissionString)
                    users.add(FullUser(name, id, permissions, hash))
                }

                return users
            }
        }
    }

    override fun insertUser(user: FullUser) {
        synchronized(createUser) {
            try {
                createUser.clearParameters()
                createUser.setString(1, user.id)
                createUser.setString(2, user.name)
                createUser.setString(3, user.hash)
                createUser.setString(4, "")
                createUser.execute()
            } catch (e: SQLException) {
                throw DuplicateUserException(e)
            }
        }
    }

    override fun updatePassword(user: FullUser) {
        synchronized(updatePassword) {
            updatePassword.clearParameters()
            updatePassword.setString(1, user.hash)
            updatePassword.setString(2, user.id)
            updatePassword.execute()
        }
    }

    override fun updatePermissions(id: String, permissions: Set<Permission>) {
        synchronized(updatePermissions) {
            updatePermissions.clearParameters()
            val permissionString = permissions.joinToString(",") { it.label }
            updatePermissions.setString(1, permissionString)
            updatePermissions.setString(2, id)
            updatePermissions.execute()
        }
    }

    override fun deleteUser(id: String) {
        synchronized(deleteUser) {
            deleteUser.clearParameters()
            deleteUser.setString(1, id)
            deleteUser.execute()
        }
    }

    override fun close() {
        connection.close()
    }
}
