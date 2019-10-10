package net.bjoernpetersen.musicbot.internal.auth

import com.google.common.base.Splitter
import net.bjoernpetersen.musicbot.api.auth.DuplicateUserException
import net.bjoernpetersen.musicbot.api.auth.FullUser
import net.bjoernpetersen.musicbot.api.auth.Permission
import net.bjoernpetersen.musicbot.api.auth.UserNotFoundException
import net.bjoernpetersen.musicbot.api.auth.createSignatureKey
import net.bjoernpetersen.musicbot.api.auth.toId
import net.bjoernpetersen.musicbot.spi.auth.UserDatabase
import java.nio.file.Path
import java.sql.DriverManager
import java.sql.SQLException
import java.util.HashSet

private const val DATABASE_VERSION = 1

@Suppress("TooManyFunctions")
internal class DefaultDatabase
@Deprecated("Use file constructor instead")
constructor(databaseUrl: String) : UserDatabase {
    @Suppress("DEPRECATION")
    constructor(file: Path) : this("jdbc:sqlite:$file")

    private val connection = DriverManager.getConnection(databaseUrl)

    init {
        tryCreateDB()
        migrateDB()
    }

    private val getUser = connection.prepareStatement(
        "SELECT name, password, permissions, signature FROM users WHERE id=?"
    )
    private val getUsers = connection.prepareStatement("SELECT * FROM users")
    private val createUser = connection.prepareStatement(
        "INSERT OR ABORT INTO users(id, name, password, permissions, signature) VALUES(?, ?, ?, ?, ?)"
    )
    private val updatePassword = connection.prepareStatement(
        "UPDATE users SET password=? WHERE id=?"
    )
    private val updatePermissions = connection.prepareStatement(
        "UPDATE users SET permissions=? WHERE id=?"
    )
    private val updateSignature = connection.prepareStatement(
        "UPDATE users SET signature=? WHERE id=?"
    )
    private val deleteUser = connection.prepareStatement("DELETE FROM users WHERE id=?")

    private fun tryCreateDB() {
        connection.createStatement().use { statement ->
            val exists = statement.executeQuery(
                "SELECT * FROM sqlite_master WHERE type='table' and name='users'"
            ).next()
            if (!exists) {
                statement.execute(
                    """CREATE TABLE IF NOT EXISTS users(
                    id TEXT PRIMARY KEY UNIQUE NOT NULL,
                    name TEXT NOT NULL,
                    password TEXT NOT NULL,
                    permissions TEXT NOT NULL,
                    signature TEXT NOT NULL)""".trimMargin()
                )
                statement.execute("PRAGMA user_version = $DATABASE_VERSION")
            }
        }
    }

    private fun migrateDB() {
        val savepoint = connection.setSavepoint()
        try {
            val version = connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA user_version").use { it.getInt(1) }
            }
            if (version < 1) {
                migrateV0()
            }
            connection.commit()
        } catch (e: SQLException) {
            connection.rollback(savepoint)
            throw SQLException("Unable to migrate database. rolling back", e)
        }
        connection.autoCommit = true
    }

    private fun migrateV0() {
        connection.createStatement().use { statement ->
            statement.execute("ALTER TABLE users ADD COLUMN signature TEXT")
            val updateSig = connection.prepareStatement("UPDATE users SET signature=? WHERE id=?")
            statement.executeQuery("SELECT * FROM users").use { resultSet ->
                while (resultSet.next()) {
                    updateSig.clearParameters()
                    updateSig.setString(1, createSignatureKey())
                    updateSig.setString(2, resultSet.getString("id"))
                    updateSig.execute()
                }
            }
            statement.execute("PRAGMA foreign_key = off")
            statement.execute(
                """CREATE TABLE IF NOT EXISTS users_copy (
                            id TEXT PRIMARY KEY UNIQUE NOT NULL,
                            name TEXT NOT NULL,
                            password TEXT NOT NULL,
                            permissions TEXT NOT NULL,
                            signature TEXT NOT NULL)""".trimIndent())
            statement.execute(
                """INSERT INTO users_copy(id, name, password, permissions, signature)
                            SELECT id, name, password, permissions, signature
                            FROM users""".trimIndent())
            statement.execute("DROP TABLE users")
            statement.execute("ALTER TABLE users_copy RENAME TO users")
            statement.execute("PRAGMA user_version = 1")
            statement.execute("PRAGMA foreign_key=on")
        }
    }

    private fun getPermissions(permissionString: String): Set<Permission> = Splitter.on(',')
        .omitEmptyStrings()
        .split(permissionString)
        .mapTo(HashSet()) { Permission.matchByLabel(it) }

    override fun findUser(name: String): FullUser {
        val id = name.toId()
        synchronized(getUser) {
            try {
                getUser.clearParameters()
                getUser.setString(1, id)
                var dbName: String
                var hash: String
                var permissionString: String
                var signature: String
                getUser.executeQuery().use { resultSet ->
                    if (!resultSet.next()) {
                        throw UserNotFoundException("No such user: $id")
                    }
                    dbName = resultSet.getString("name")
                    hash = resultSet.getString("password")
                    permissionString = resultSet.getString("permissions")
                    val permissions = getPermissions(permissionString)
                    signature = resultSet.getString("signature")
                    return FullUser(dbName, permissions, signature, hash)
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
                    val name = resultSet.getString("name")
                    val hash = resultSet.getString("password")
                    val permissionString = resultSet.getString("permissions")
                    val permissions = getPermissions(permissionString)
                    val signature = resultSet.getString("signature")
                    users.add(FullUser(name, permissions, signature, hash))
                }

                return users
            }
        }
    }

    @Suppress("MagicNumber")
    override fun insertUser(user: FullUser, hash: String) {
        val id = user.name.toId()
        val permissionString = user.permissions.joinToString(",") { it.label }
        synchronized(createUser) {
            try {
                createUser.clearParameters()
                createUser.setString(1, id)
                createUser.setString(2, user.name)
                createUser.setString(3, hash)
                createUser.setString(4, permissionString)
                createUser.setString(5, user.signature)
                createUser.execute()
            } catch (e: SQLException) {
                throw DuplicateUserException(e)
            }
        }
    }

    @Suppress("MagicNumber")
    override fun updatePassword(name: String, hash: String) {
        val id = name.toId()
        synchronized(updatePassword) {
            updatePassword.clearParameters()
            updatePassword.setString(1, hash)
            updatePassword.setString(2, id)
            updatePassword.execute()
            if (updatePassword.updateCount == 0)
                throw UserNotFoundException("Can't update user because it does not exist: $name")
        }
    }

    @Suppress("MagicNumber")
    override fun updatePermissions(name: String, permissions: Set<Permission>) {
        val id = name.toId()
        synchronized(updatePermissions) {
            updatePermissions.clearParameters()
            val permissionString = permissions.joinToString(",") { it.label }
            updatePermissions.setString(1, permissionString)
            updatePermissions.setString(2, id)
            updatePermissions.execute()
            if (updatePermissions.updateCount == 0)
                throw UserNotFoundException("Can't update user because it does not exist: $name")
        }
    }

    override fun updateSignature(name: String, signature: String) {
        val id = name.toId()
        synchronized(updateSignature) {
            updateSignature.clearParameters()
            updateSignature.setString(1, signature)
            updateSignature.setString(2, id)
            updateSignature.execute()
            if (updatePermissions.updateCount == 0)
                throw UserNotFoundException("Can't update user because it does not exist: $name")
        }
    }

    override fun deleteUser(name: String) {
        val id = name.toId()
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
