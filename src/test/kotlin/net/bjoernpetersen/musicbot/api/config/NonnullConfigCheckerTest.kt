package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.test.api.config.ConfigExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ConfigExtension::class)
class NonnullConfigCheckerTest {

    @Test
    fun exactType(config: Config) {
        config.SerializedEntry(
            "key",
            "TestEntry",
            object : ConfigSerializer<Any> {
                override fun serialize(obj: Any): String = obj.toString()
                override fun deserialize(string: String): Any = string
            },
            NonnullConfigChecker)
    }

    @Test
    fun subType(config: Config) {
        config.StringEntry(
            "key",
            "TestEntry",
            NonnullConfigChecker)
    }

    @Test
    fun noWarningForBlank() {
        assertNull(NonnullConfigChecker(""))
        assertNull(NonnullConfigChecker(" "))
    }

    @Test
    fun noWarningForString() {
        assertNull(NonnullConfigChecker("actual value"))
    }

    @Test
    fun warningForNull() {
        assertThat(NonnullConfigChecker(null))
            .isNotNull()
            .isNotBlank()
    }
}
