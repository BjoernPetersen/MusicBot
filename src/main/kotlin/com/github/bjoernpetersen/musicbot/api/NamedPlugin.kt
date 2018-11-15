package com.github.bjoernpetersen.musicbot.api

import com.github.bjoernpetersen.musicbot.spi.plugin.Plugin
import kotlin.reflect.KClass

data class NamedPlugin<out T : Plugin>(val id: KClass<out T>, val name: String)
