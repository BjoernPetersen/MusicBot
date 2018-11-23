package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.spi.config.ConfigStorageAdapter
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class ConfigExtension : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext,
        extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == Config::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext,
        extensionContext: ExtensionContext): Any {
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
