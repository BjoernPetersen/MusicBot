package net.bjoernpetersen.musicbot.api.player

import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.jupiter.api.Test

class SongTest {
    @Test
    fun equalsContract() {
        EqualsVerifier.forClass(Song::class.java)
            .withOnlyTheseFields("id", "provider")
            .verify()
    }
}
