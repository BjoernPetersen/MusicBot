package net.bjoernpetersen.musicbot.api.plugin

import mu.KotlinLogging
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.util.LinkedList
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import kotlin.reflect.KClass

class PluginLoader(private val pluginFolder: File) {

    private val logger = KotlinLogging.logger {}
    val loader: ClassLoader by lazy { createLoader() }

    @Throws(MalformedURLException::class)
    private fun createLoader(): ClassLoader {
        if (!pluginFolder.isDirectory) {
            if (!pluginFolder.mkdirs()) {
                throw PluginLoadingException(
                    "Could not create plugin folder '${pluginFolder.path}'"
                )
            }
        }

        val files = pluginFolder
            .listFiles { path -> path.name.endsWith(".jar") }
            ?: throw PluginLoadingException()

        val urls = arrayOfNulls<URL>(files.size)
        for (i in files.indices) {
            urls[i] = files[i].toURI().toURL()
        }

        return URLClassLoader(urls, javaClass.classLoader)
    }

    fun <T : Plugin> load(type: KClass<T>): Collection<T> {
        val result = LinkedList<T>()
        try {
            val plugins = loadPlugins(type, loader)
            result.addAll(plugins)
            logger.info {
                "Loaded ${plugins.size} plugins of type '${type.simpleName}' from plugin folder: ${pluginFolder.name}"
            }
        } catch (e: Exception) {
            logger.error("Error loading plugins", e)
        } catch (e: Error) {
            logger.error("Error loading plugins", e)
        }
        return result
    }

    @Throws(ServiceConfigurationError::class, NoClassDefFoundError::class)
    private fun <T : Plugin> loadPlugins(type: KClass<T>, classLoader: ClassLoader): Collection<T> {
        val loader = ServiceLoader.load<T>(type.java, classLoader)

        val result = LinkedList<T>()
        for (plugin in loader) {
            if (type.isInstance(plugin)) {
                result.add(plugin)
            } else logger.error {
                "Loaded plugin '${plugin::class.simpleName}' is not instance of desired type ${type.simpleName}"
            }
        }
        return result
    }
}

class PluginLoadingException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
