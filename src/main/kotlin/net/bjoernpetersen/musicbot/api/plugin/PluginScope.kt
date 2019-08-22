package net.bjoernpetersen.musicbot.api.plugin

import com.google.common.annotations.Beta
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * Defines a standard scope which plugins can use for delegation.
 *
 * ### Example usage
 *
 * ```
 * class MyPlugin : Plugin, CoroutineScope by PluginScope() {
 *     private fun cancelScope() {
 *         cancel()
 *     }
 *
 *     override suspend fun close() {
 *         cancelScope()
 *     }
 * }
 * ```
 */
@Beta
class PluginScope private constructor(
    override val coroutineContext: CoroutineContext
) : CoroutineScope {
    companion object {
        /**
         * Creates a [PluginScope] to be used by a plugin for delegation.
         */
        operator fun invoke(dispatcher: CoroutineDispatcher = Dispatchers.Default): CoroutineScope {
            return PluginScope(SupervisorJob() + dispatcher)
        }
    }
}
