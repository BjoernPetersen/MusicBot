package net.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import javax.inject.Named

/**
 * Binds the given ClassLoader as the `PluginClassLoader`.
 *
 * To inject the ClassLoader somewhere, annotate the injection target with
 * [`@Named("PluginClassLoader")`][Named]
 *
 * @param classLoader the ClassLoader the plugins have been loaded with
 */
class PluginClassLoaderModule(private val classLoader: ClassLoader) : AbstractModule() {

    override fun configure() {
        bind(ClassLoader::class.java)
            .annotatedWith(Names.named("PluginClassLoader"))
            .toInstance(classLoader)
    }
}
