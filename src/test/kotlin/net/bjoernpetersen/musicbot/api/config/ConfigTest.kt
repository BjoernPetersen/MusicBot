package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.spi.config.ConfigChecker
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
            ImplSerializer,
            { null },
            ActionButton(
                "Label",
                ::baseDescriptor) {
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
            ImplSerializer,
            checker,
            ActionButton(
                "Label",
                ::baseDescriptor) {
                true
            })
    }

    private fun implDescriptor(impl: Impl) = baseDescriptor(impl)

    private fun baseDescriptor(base: Base): String {
        return base.name
    }
}

private object ImplSerializer : ConfigSerializer<Impl> {
    override fun serialize(obj: Impl): String {
        return obj.name
    }

    override fun deserialize(string: String): Impl {
        return Impl(string)
    }
}

private interface Base {
    val name: String
}

data class Impl(override val name: String = "TestName") : Base
