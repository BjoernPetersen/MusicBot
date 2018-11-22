package com.github.bjoernpetersen.musicbot.api.plugin.management

import com.github.bjoernpetersen.musicbot.api.config.Config
import com.github.bjoernpetersen.musicbot.api.config.ConfigExtension
import com.github.bjoernpetersen.musicbot.spi.plugin.Bases
import com.github.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import com.github.bjoernpetersen.musicbot.spi.plugin.IdBase
import com.github.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ConfigExtension::class)
class DefaultPluginManagerTest {

    @Test
    fun correctDefaults(config: Config) {
        val manager = DefaultPluginManager(
            config,
            listOf(TestPlugin),
            emptyList(),
            emptyList(),
            emptyList())

        assertTrue(manager.isEnabled(TestPlugin))
        assertFalse(manager.isEnabled(TestPlugin, TestPluginInterface::class))
    }

    @Test
    fun enableDefault(config: Config) {
        val manager = DefaultPluginManager(
            config,
            listOf(TestPlugin),
            emptyList(),
            emptyList(),
            emptyList())

        manager.setEnabled(TestPlugin, TestPluginInterface::class)
        assertTrue(manager.isEnabled(TestPlugin, TestPluginInterface::class))
    }
}

private interface TestPluginInterface : GenericPlugin

@IdBase(TestPluginInterface::class)
@Bases(GenericPlugin::class, TestPluginInterface::class)
private object TestPlugin : TestPluginInterface {

    override val name: String = "TestPlugin"

    override val description: String = "Unit test description"

    override fun initialize(initStateWriter: InitStateWriter) {
    }

    override fun createConfigEntries(config: Config): List<Config.Entry<*>> = emptyList()

    override fun createSecretEntries(secrets: Config): List<Config.Entry<*>> = emptyList()

    override fun createStateEntries(state: Config) {
    }

    override fun close() {

    }

}
