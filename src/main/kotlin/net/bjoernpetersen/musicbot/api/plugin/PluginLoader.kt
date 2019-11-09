package net.bjoernpetersen.musicbot.api.plugin

import java.io.File
import java.util.ServiceLoader
import kotlin.reflect.KClass
import net.bjoernpetersen.musicbot.spi.plugin.Plugin

/**
 * Responsible for plugin discovery.
 *
 * This should usually happen using the [PluginLoaderImpl], which uses Java's [ServiceLoader].
 */
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

    companion object {
        /**
         * Temporary method to ease the transition from PluginLoader being a final class to
         * it being an interface.
         */
        @Deprecated("Use PluginLoaderImpl", ReplaceWith("PluginLoaderImpl()"))
        operator fun invoke(pluginFolder: File): PluginLoader = PluginLoaderImpl(pluginFolder)
    }
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
