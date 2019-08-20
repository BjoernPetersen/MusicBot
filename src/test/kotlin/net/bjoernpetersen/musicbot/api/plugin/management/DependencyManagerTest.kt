package net.bjoernpetersen.musicbot.api.plugin.management

import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.test.api.config.ConfigExtension
import net.bjoernpetersen.musicbot.api.plugin.management.plugins.AuthMyProvider
import net.bjoernpetersen.musicbot.api.plugin.management.plugins.AuthMySuggester
import net.bjoernpetersen.musicbot.api.plugin.management.plugins.DumbAuth
import net.bjoernpetersen.musicbot.api.plugin.management.plugins.DumbAuthImpl
import net.bjoernpetersen.musicbot.api.plugin.management.plugins.MyProvider
import net.bjoernpetersen.musicbot.api.plugin.management.plugins.MyProviderImpl
import net.bjoernpetersen.musicbot.api.plugin.management.plugins.SelfIdActiveGeneric
import net.bjoernpetersen.musicbot.api.plugin.management.plugins.SelfIdGeneric
import net.bjoernpetersen.musicbot.api.plugin.management.plugins.SelfIdPlayback
import net.bjoernpetersen.musicbot.api.plugin.management.plugins.SelfIdProvider
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.Suggester
import net.bjoernpetersen.musicbot.spi.plugin.bases
import net.bjoernpetersen.musicbot.spi.plugin.category
import net.bjoernpetersen.musicbot.spi.plugin.id
import net.bjoernpetersen.musicbot.spi.plugin.management.DependencyManager
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ConfigExtension::class)
class DependencyManagerTest {

    private fun manager(
        state: Config,
        genericPlugins: List<GenericPlugin> = emptyList(),
        playbackFactories: List<PlaybackFactory> = emptyList(),
        providers: List<Provider> = emptyList(),
        suggesters: List<Suggester> = emptyList()
    ): DependencyManager = DefaultDependencyManager(
        state,
        genericPlugins,
        playbackFactories,
        providers,
        suggesters)

    private fun GenericPlugin.managed(config: Config): Pair<GenericPlugin, DependencyManager> {
        return this to manager(config, genericPlugins = listOf(this))
    }

    private fun PlaybackFactory.managed(config: Config): Pair<PlaybackFactory, DependencyManager> {
        return this to manager(config, playbackFactories = listOf(this))
    }

    private fun Provider.managed(config: Config): Pair<Provider, DependencyManager> {
        return this to manager(config, providers = listOf(this))
    }

    @TestFactory
    fun activePluginsAreActive(config: Config) = listOf(
        SelfIdActiveGeneric().managed(config),
        SelfIdProvider().managed(config))
        .map { (plugin, manager) ->
            dynamicTest("${plugin.category.simpleName}: ${plugin.name}") {
                assertFalse(manager.isEnabled(plugin))
                manager.setDefault(plugin, plugin.id)
                assertTrue(manager.isEnabled(plugin))
            }
        }

    @TestFactory
    fun inactivePluginsAreInactive(config: Config) = listOf(
        SelfIdGeneric().managed(config),
        DumbAuthImpl().managed(config),
        SelfIdPlayback().managed(config))
        .map { (plugin, manager) ->
            dynamicTest("${plugin.category.simpleName}: ${plugin.name}") {
                assertFalse(manager.isEnabled(plugin))
                plugin.bases.forEach { manager.setDefault(plugin, it) }
                assertFalse(manager.isEnabled(plugin))
            }
        }

    @Test
    fun simpleDependency(config: Config) {
        val auth = DumbAuthImpl()
        val provider = AuthMyProvider()
        val manager = manager(
            config,
            genericPlugins = listOf(auth),
            providers = listOf(provider))

        assertFalse(manager.isEnabled(provider))
        manager.setDefault(provider, provider.id)
        assertTrue(manager.isEnabled(provider))

        assertFalse(manager.isEnabled(auth))
        manager.setDefault(auth, DumbAuth::class)
        assertTrue(manager.isEnabled(auth))
    }

    @Test
    fun transitiveDependency(config: Config) {
        val auth = DumbAuthImpl()
        val authedProvider = AuthMyProvider()
        val unauthedProvider = MyProviderImpl()
        val suggester = AuthMySuggester()
        val manager = manager(
            config,
            genericPlugins = listOf(auth),
            providers = listOf(authedProvider, unauthedProvider),
            suggesters = listOf(suggester))

        assertFalse(manager.isEnabled(auth))
        manager.setDefault(auth, DumbAuth::class)
        assertFalse(manager.isEnabled(auth))

        assertFalse(manager.isEnabled(authedProvider))
        assertFalse(manager.isEnabled(unauthedProvider))

        assertFalse(manager.isEnabled(suggester))
        manager.setDefault(suggester, AuthMySuggester::class)
        assertTrue(manager.isEnabled(suggester))

        assertFalse(manager.isEnabled(auth))

        manager.setDefault(authedProvider, MyProvider::class)
        assertTrue(manager.isEnabled(auth))

        manager.setDefault(unauthedProvider, MyProvider::class)
        assertFalse(manager.isEnabled(auth))
    }
}
