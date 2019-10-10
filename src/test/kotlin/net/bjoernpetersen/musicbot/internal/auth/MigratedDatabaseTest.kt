package net.bjoernpetersen.musicbot.internal.auth

import com.google.inject.Injector
import net.bjoernpetersen.musicbot.api.auth.User
import net.bjoernpetersen.musicbot.api.auth.toId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class MigratedDatabaseTest : DefaultDatabaseTest() {
    private lateinit var connection: Connection;

    private var injectorFunctions: List<() -> Unit> = emptyList()
    private fun execute(functions: List<() -> Unit>) {
        functions.forEach {
            it()
        }
    }

    @BeforeEach
    override fun createDb(injector: Injector) {
        file = Files.createTempFile(Paths.get(DIR), FILE_NAME, FILE_EXTENSION)
        connection = DriverManager.getConnection("jdbc:sqlite:$file")
        connection.createStatement().use { statement ->
            statement.execute(
                """CREATE TABLE IF NOT EXISTS users(
                    id TEXT PRIMARY KEY UNIQUE NOT NULL,
                    name TEXT NOT NULL,
                    password TEXT NOT NULL,
                    permissions TEXT NOT NULL)""".trimMargin()
            )
        }
        execute(injectorFunctions)
        database = injector.createDatabase(file)
    }

    @AfterEach
    override fun closeDb() {
        assertVersion1()
        connection.close()
        super.closeDb()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FilledV0Database {

        @BeforeAll
        fun insertV0Users() {
            injectorFunctions = listOf { insertUsers() }
        }

        @TestFactory
        fun findUser(): List<DynamicTest> {
            return KNOWN_USER_VARIATIONS.map {
                DynamicTest.dynamicTest("\"$it\"") {
                    val user = assertDoesNotThrow { database.findUser(it) }
                    val expected = users.first()
                    assertEquals(expected.name, user.name)
                    assertEquals(expected.permissions, user.permissions)
                    assertThat(user.hasPassword(expected.getPass()))
                }
            }
        }

    }

    private fun assertVersion1() {
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA user_version")
                .use { it.getInt(1) }
                .also { Assertions.assertEquals(1, it) }

            statement.executeQuery("PRAGMA table_info(users)").use {
                var count = 0;
                while (it.next()) {
                    val column = v1Columns[it.getString("name")]
                        ?: throw SQLException("unexpected column ${it.getString("name")} in db")
                    Assertions.assertEquals(column.type, it.getString("type"))
                    Assertions.assertEquals(column.notnull, it.getBoolean("notnull"))
                    Assertions.assertEquals(column.pk, it.getBoolean("pk"))
                    count++
                }
                Assertions.assertEquals(v1Columns.size, count)
            }
        }
    }

    private fun insertUsers() {
        val insertV0 = connection.prepareStatement(
            "INSERT OR ABORT INTO users(id, name, password, permissions) VALUES(?, ?, ?, ?)")
        users.forEach { user ->
            insertV0.clearParameters()
            insertV0.setString(1, user.name.toId())
            insertV0.setString(2, user.name)
            insertV0.setString(3, user.hash())
            insertV0.setString(4, user.permissions.joinToString(",") { it.label })
            insertV0.execute()
        }
    }

    private companion object {
        const val TEXT = "TEXT"
        val v1Columns = mapOf(
            "id" to DBColumn(TEXT, true, true),
            "name" to DBColumn(TEXT, true, false),
            "password" to DBColumn(TEXT, true, false),
            "permissions" to DBColumn(TEXT, true, false),
            "signature" to DBColumn(TEXT, true, false)
        )

        fun User.hash(): String {
            return when (this) {
                users[0] -> HASH
                users[1] -> HASH2
                else -> throw IllegalArgumentException()
            }
        }
    }

    private data class DBColumn(
        val type: String,
        val notnull: Boolean,
        val pk: Boolean
    ) {}

}
