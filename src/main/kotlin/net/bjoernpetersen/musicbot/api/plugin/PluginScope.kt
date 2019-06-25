package net.bjoernpetersen.musicbot.api.plugin

import com.google.common.annotations.Beta
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

@Beta
class PluginScope private constructor(
    override val coroutineContext: CoroutineContext
) : CoroutineScope {
    companion object {
        operator fun invoke(dispatcher: CoroutineDispatcher = Dispatchers.Default): CoroutineScope {
            return PluginScope(SupervisorJob() + dispatcher)
        }
    }
}
