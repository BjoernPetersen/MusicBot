package net.bjoernpetersen.musicbot.api.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FileChooserTest {
    @Test
    fun `isDirectory and isFile`() {
        FileChooser(isDirectory = true, isOpen = true)
    }

    @Test
    fun `not isDirectory and isFile`() {
        FileChooser(isDirectory = false, isOpen = true)
    }

    @Test
    fun `isDirectory and not isFile`() {
        assertThrows<IllegalArgumentException> {
            FileChooser(isDirectory = true, isOpen = false)
        }
    }

    @Test
    fun `not isDirectory and not isFile`() {
        FileChooser(isDirectory = false, isOpen = false)
    }
}
