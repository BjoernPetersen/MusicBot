package com.github.bjoernpetersen.musicbot.api.module

import com.google.inject.AbstractModule
import com.google.inject.Provides
import javax.inject.Singleton

class PluginClassLoaderModule(private val classLoader: ClassLoader) : AbstractModule() {
    @Provides
    @Singleton
    fun provideClassLoader(): ClassLoader = classLoader
}
