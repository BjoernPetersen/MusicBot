package net.bjoernpetersen.musicbot.api.plugin

import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import kotlin.reflect.KClass

interface PluginLoader {
    /**
     * The ClassLoader used to load the plugins.
     */
    val loader: ClassLoader

    /**
     * Loads all plugins extending the specified [type].
     *
     * @param type a service interface
     * @return all found plugins registered for that service type
     */
    @Throws(PluginLoadingException::class)
    fun <T : Plugin> load(type: KClass<T>): Collection<T>
}

/**
 * Thrown if a plugin could not be loaded.
 */
class PluginLoadingException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
