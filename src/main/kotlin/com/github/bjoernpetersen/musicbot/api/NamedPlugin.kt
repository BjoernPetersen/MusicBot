package com.github.bjoernpetersen.musicbot.api

import com.github.bjoernpetersen.musicbot.spi.plugin.Plugin
import kotlin.reflect.KClass

data class NamedPlugin<out T : Plugin>(val id: String, val name: String) {
    constructor(idClass: Class<out T>, name: String) : this(idClass.name, name)
    constructor(idClass: KClass<out T>, name: String) : this(idClass.java, name)
}
