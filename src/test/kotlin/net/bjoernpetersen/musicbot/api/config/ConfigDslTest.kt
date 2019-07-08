package net.bjoernpetersen.musicbot.api.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ConfigExtension::class)
class ConfigDslTest {
    @ExperimentalConfigDsl
    @Test
    fun serializerContravariant(config: Config) {
        val serializeAny = { any: Any -> any.toString() }
        config.implEntry {
            serialization {
                serialize(serializeAny)
                deserialize { Impl.deserialize(it) }
            }
        }
    }
}
