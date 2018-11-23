package net.bjoernpetersen.musicbot.api.config

import kotlin.reflect.KClass

sealed class ConfigScope(private val scopeString: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConfigScope) return false
        if (scopeString != other.scopeString) return false
        return true
    }

    override fun hashCode(): Int {
        return scopeString.hashCode()
    }

    override fun toString(): String {
        return scopeString
    }
}

class PluginConfigScope(type: KClass<out Any>) : ConfigScope(type.qualifiedName!!)
class GenericConfigScope(type: KClass<out Any>) : ConfigScope(type.qualifiedName!!)
object MainConfigScope : ConfigScope("Main")
