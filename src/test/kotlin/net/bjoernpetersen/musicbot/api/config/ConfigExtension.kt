package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.spi.config.ConfigStorageAdapter
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * Injects an in-memory config as a parameter.
 */
class ConfigExtension : ParameterResolver {

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean {
        return parameterContext.parameter.type == Config::class.java
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return Config(MemoryConfigAdapter(), MainConfigScope)
    }
}

private class MemoryConfigAdapter : ConfigStorageAdapter {
    private val entries: MutableMap<ConfigScope, Map<String, String>> = HashMap()

    override fun load(scope: ConfigScope): Map<String, String> {
        return entries[scope]?.toMap() ?: emptyMap()
    }

    override fun store(scope: ConfigScope, config: Map<String, String>) {
        entries[scope] = config.toMap()
    }
}

@UseExperimental(ExperimentalConfigDsl::class)
fun Config.implEntry(
    key: String = "key",
    configure: SerializedConfiguration<Impl>.() -> Unit = {}
): Config.SerializedEntry<Impl> {
    return serialized(key) {
        description = "description"
        serializer = Impl
        check { null }
        actionButton {
            label = "Label"
            describe { it.name }
            action { true }
        }
        configure()
    }
}

interface Base {
    val name: String
}

data class Impl(override val name: String = "TestName") : Base {
    companion object : ConfigSerializer<Impl> {
        override fun serialize(obj: Impl): String {
            return obj.name
        }

        override fun deserialize(string: String): Impl {
            return Impl(string)
        }
    }

    object FaultySerializer : ConfigSerializer<Impl> {
        override fun serialize(obj: Impl): String {
            return obj.name
        }

        override fun deserialize(string: String): Impl {
            throw SerializationException()
        }
    }
}
