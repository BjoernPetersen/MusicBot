package net.bjoernpetersen.musicbot.spi.config

import com.google.common.annotations.Beta
import net.bjoernpetersen.musicbot.api.plugin.ActiveBase
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.plugin.GenericPlugin

/**
 * A kind of [GenericPlugin] providing a ConfigStorageAdapter implementation.
 *
 * Implementations should still be registered as a `GenericPlugin` in the services manifest.
 */
@Beta
@IdBase("Config storage")
@ActiveBase
interface ConfigStorageAdapterPlugin : GenericPlugin {
    /**
     * @return A ConfigStorageAdapter.
     */
    fun createAdapter(): ConfigStorageAdapter
}
