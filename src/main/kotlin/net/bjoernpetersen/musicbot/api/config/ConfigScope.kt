package net.bjoernpetersen.musicbot.api.config

import kotlin.reflect.KClass

/**
 * A config scope.
 * Scopes are generally distinguished by the [scopeString], but since this is a sealed class
 * the subtype may also be incorporated.
 */
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

/**
 * A config scope for plugin configuration.
 * Only the config of a single plugin should be stored in this scope.
 *
 * @param type the plugin class
 */
class PluginConfigScope(type: KClass<out Any>) : ConfigScope(type.qualifiedName!!)

/**
 * A generic config scope for any class. Generally only used by the core-lib and its implementation.
 */
class GenericConfigScope(type: KClass<out Any>) : ConfigScope(type.qualifiedName!!)

/**
 * Config scope for entries of global importance in the core-lib.
 */
object MainConfigScope : ConfigScope("Main")
