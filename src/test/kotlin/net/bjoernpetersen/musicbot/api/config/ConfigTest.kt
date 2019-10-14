package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.spi.config.ConfigChecker
import net.bjoernpetersen.musicbot.test.api.config.ConfigExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ConfigExtension::class)
class ConfigTest {

    @Test
    fun uiVariance(config: Config) {
        @Suppress("UNUSED_VARIABLE")
        val entry: Config.SerializedEntry<Impl> = config.SerializedEntry(
            "key",
            "description",
            Impl,
            { null },
            ActionButton(
                "Label",
                ::baseDescriptor
            ) {
                true
            })
    }

    @Test
    fun checkerVariance(config: Config) {
        val checker: ConfigChecker<Base> = { null }

        @Suppress("UNUSED_VARIABLE")
        val entry: Config.SerializedEntry<Impl> = config.SerializedEntry(
            "key",
            "description",
            Impl,
            checker,
            ActionButton(
                "Label",
                ::baseDescriptor
            ) {
                true
            })
    }

    @Test
    fun serializeNull(config: Config) {
        val entry = config.implEntry()
        entry.set(null)
    }

    @Test
    fun serialize(config: Config) {
        val entry = config.implEntry()
        val impl = Impl("test")
        entry.set(impl)
        assertEquals(impl, entry.get())
    }

    @Test
    fun deserializeNull(config: Config) {
        val entry = config.implEntry()
        assertNull(entry.get())
    }

    @Test
    fun deserializeError(config: Config) {
        val entry = config.implEntry {
            serializer = Impl.FaultySerializer
        }
        entry.set(Impl("test"))
        assertNull(entry.get())
    }

    private fun baseDescriptor(base: Base): String {
        return base.name
    }
}
