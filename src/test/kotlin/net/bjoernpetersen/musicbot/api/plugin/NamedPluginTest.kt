package net.bjoernpetersen.musicbot.api.plugin

import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.jupiter.api.Test

class NamedPluginTest {
    @Test
    fun equalsValid() {
        EqualsVerifier.forClass(NamedPlugin::class.java)
            .withOnlyTheseFields("id")
            .verify()
    }
}
