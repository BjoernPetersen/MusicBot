package net.bjoernpetersen.musicbot.api.player

import net.bjoernpetersen.musicbot.api.auth.GuestUser
import net.bjoernpetersen.musicbot.api.auth.User
import net.bjoernpetersen.musicbot.api.plugin.NamedPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test

class QueueEntryTest {

    private fun user(name: String): User = GuestUser(name, "testid")

    private fun song(id: String, providerId: String) = Song(
        id,
        NamedPlugin(providerId, "providerSubject"),
        "songTitle",
        "songDescription",
        60,
        null
    )

    @Test
    fun shallowEqual() {
        val user = user("testuser")
        val song = song("songId", "providerId")
        val entryA = QueueEntry(song, user)
        val entryB = QueueEntry(song, user)
        assertEquals(entryA, entryB)
    }

    @Test
    fun deepEqual() {
        val userA = user("testuser")
        val userB = user("testuser")
        assertNotSame(userA, userB)

        val songA = song("songId", "providerId")
        val songB = songA.copy()
        assertNotSame(songA, songB)

        val entryA = QueueEntry(songA, userA)
        val entryB = QueueEntry(songB, userB)

        assertEquals(entryA, entryB)
    }
}
