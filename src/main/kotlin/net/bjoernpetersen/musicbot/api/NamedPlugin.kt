package net.bjoernpetersen.musicbot.api

import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import kotlin.reflect.KClass

data class NamedPlugin<out T : Plugin>(val id: String, val subject: String) {
    constructor(idClass: KClass<out T>, name: String) : this(idClass.java.name, name)
}
