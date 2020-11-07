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

/**
 * Loads plugins from jar-files from the specified folder using Java's [ServiceLoader].
 *
 * @param pluginFolder a folder directly containing plugin jars.
 */
class PluginLoaderImpl(private val pluginFolder: File) : PluginLoader {

    private val logger = KotlinLogging.logger {}

    /**
     * The ClassLoader used to load the plugins.
     */
    override val loader: ClassLoader by lazy { createLoader() }

    @Throws(MalformedURLException::class, PluginLoadingException::class)
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

    /**
     * Loads all plugins extending the specified [type].
     *
     * @param type a service interface
     * @return all found plugins registered for that service type
     */
    @Throws(PluginLoadingException::class)
    @Suppress("TooGenericExceptionCaught")
    override fun <T : Plugin> load(type: KClass<T>): Collection<T> {
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
