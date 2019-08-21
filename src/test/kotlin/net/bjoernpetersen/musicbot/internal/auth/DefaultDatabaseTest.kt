package net.bjoernpetersen.musicbot.internal.auth

import com.google.inject.Injector
import name.falgout.jeffrey.testing.junit.guice.GuiceExtension
import net.bjoernpetersen.musicbot.api.auth.DuplicateUserException
import net.bjoernpetersen.musicbot.api.auth.FullUser
import net.bjoernpetersen.musicbot.api.auth.Permission
import net.bjoernpetersen.musicbot.api.auth.User
import net.bjoernpetersen.musicbot.api.auth.UserNotFoundException
import net.bjoernpetersen.musicbot.api.module.DefaultUserDatabaseModule
import net.bjoernpetersen.musicbot.spi.auth.UserDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mindrot.jbcrypt.BCrypt
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@ExtendWith(GuiceExtension::class)
class DefaultDatabaseTest {
    private lateinit var file: Path
    private lateinit var database: UserDatabase

    private fun Injector.createDatabase(url: String): UserDatabase {
        return createChildInjector(DefaultUserDatabaseModule(url))
            .getInstance(UserDatabase::class.java)
    }

    @BeforeEach
    fun createDb(injector: Injector) {
        file = Files.createTempFile(Paths.get(DIR), FILE_NAME, FILE_EXTENSION)
        database = injector.createDatabase("jdbc:sqlite:$file")
    }

    @AfterEach
    fun closeDb() {
        database.close()
        Files.delete(file)
    }

    @Test
    fun `empty getUsers`() {
        assertThat(database.getUsers())
            .isEmpty()
    }

    @Test
    fun `unknown findUser`() {
        assertThrows<UserNotFoundException> {
            assertThat(database.findUser(UNKNOWN_USER))
        }
    }

    @Test
    fun `unknown deleteUser`() {
        assertDoesNotThrow {
            database.deleteUser(UNKNOWN_USER)
        }
    }

    @Test
    fun `unknown updatePassword`() {
        assertThrows<UserNotFoundException> {
            database.updatePassword(UNKNOWN_USER, "abc")
        }
    }

    @Test
    fun `unknown updatePermissions`() {
        assertThrows<UserNotFoundException> {
            database.updatePermissions(UNKNOWN_USER, emptySet())
        }
    }

    @Nested
    inner class FilledDatabase {
        @BeforeEach
        fun fillDatabase() {
            database.insertUser(users[0], HASH)
            database.insertUser(users[1], HASH2)
        }

        @Test
        fun getUsers() {
            assertThat(database.getUsers())
                .hasSize(2)
                .containsAll(users)
                .allSatisfy {
                    val pass = it.getPass()
                    assertTrue(it.hasPassword(pass))
                }
        }

        @TestFactory
        fun findUser(): List<DynamicTest> {
            return listOf(KNOWN_USER, KNOWN_USER.toUpperCase(), "  $KNOWN_USER  ")
                .map {
                    dynamicTest("\"$it\"") {
                        val user = assertDoesNotThrow { database.findUser(it) }
                        val expected = users.first()
                        assertEquals(expected.name, user.name)
                        assertEquals(expected.permissions, user.permissions)
                    }
                }
        }

        @TestFactory
        fun insertDuplicate(): List<DynamicTest> {
            return listOf(KNOWN_USER, KNOWN_USER.toUpperCase(), "  $KNOWN_USER  ")
                .map {
                    dynamicTest("\"$it\"") {
                        assertThrows<DuplicateUserException> {
                            database.insertUser(FullUser(KNOWN_USER, emptySet(), HASH2), HASH2)
                        }
                    }
                }
        }

        @Test
        fun updatePassword() {
            database.updatePassword(KNOWN_USER, HASH2)
            val user = database.findUser(KNOWN_USER)
            assertTrue(user.hasPassword(PASS2))
        }

        @Test
        fun updatePermissions() {
            val permissions = setOf(Permission.EXIT, Permission.SKIP)
            database.updatePermissions(KNOWN_USER, permissions)
            val user = database.findUser(KNOWN_USER)
            assertEquals(permissions, user.permissions)
        }

        @Test
        fun deleteUser() {
            database.deleteUser(KNOWN_USER)
            assertThrows<UserNotFoundException> {
                database.findUser(KNOWN_USER)
            }
        }
    }

    private companion object {
        const val DIR = "build/tmp"
        const val FILE_NAME = "test"
        const val FILE_EXTENSION = "db"

        const val KNOWN_USER = "known"
        const val KNOWN_USER2 = "known2"
        const val PASS = ";jkl345;ljkdfg"
        val HASH = BCrypt.hashpw(PASS, BCrypt.gensalt())!!
        const val PASS2 = "j;23l45j*"
        val HASH2 = BCrypt.hashpw(PASS2, BCrypt.gensalt())!!
        const val UNKNOWN_USER = "unknown"

        val users = listOf(
            FullUser(
                name = KNOWN_USER,
                permissions = setOf(Permission.MOVE, Permission.DISLIKE),
                hash = HASH
            ),
            FullUser(
                name = KNOWN_USER2,
                permissions = setOf(Permission.PAUSE, Permission.ENQUEUE),
                hash = HASH2
            )
        )

        fun User.getPass(): String {
            return when (this) {
                users[0] -> PASS
                users[1] -> PASS2
                else -> throw IllegalArgumentException()
            }
        }
    }
}
