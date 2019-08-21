package net.bjoernpetersen.musicbot.api.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PathChooserTest {
    @Test
    fun `isDirectory and isFile`() {
        PathChooser(isDirectory = true, isOpen = true)
    }

    @Test
    fun `not isDirectory and isFile`() {
        PathChooser(isDirectory = false, isOpen = true)
    }

    @Test
    fun `isDirectory and not isFile`() {
        assertThrows<IllegalArgumentException> {
            PathChooser(isDirectory = true, isOpen = false)
        }
    }

    @Test
    fun `not isDirectory and not isFile`() {
        PathChooser(isDirectory = false, isOpen = false)
    }
}
