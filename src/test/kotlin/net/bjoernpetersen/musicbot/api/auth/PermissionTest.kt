package net.bjoernpetersen.musicbot.api.auth

import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class PermissionTest {
    @Test
    fun defaultsNotEmpty() {
        assertNotEquals(emptySet<Permission>(), Permission.getDefaults())
    }
}
