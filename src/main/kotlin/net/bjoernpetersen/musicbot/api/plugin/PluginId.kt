package net.bjoernpetersen.musicbot.api.plugin

import net.bjoernpetersen.musicbot.api.config.ConfigSerializer
import net.bjoernpetersen.musicbot.api.config.SerializationException
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

class PluginId(type: KClass<*>, val displayName: String) {
    val type: KClass<out Plugin>
    val qualifiedName: String
        get() = type.qualifiedName!!

    init {
        if (Plugin::class.isSuperclassOf(type)) {
            @Suppress("UNCHECKED_CAST")
            this.type = type as KClass<out Plugin>
        } else {
            throw IllegalArgumentException("ID type doesn't extend Plugin: ${type.qualifiedName}")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PluginId) return false

        if (displayName != other.displayName) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String {
        return displayName
    }

    companion object {
        operator fun invoke(idClass: KClass<*>): PluginId? = try {
            idClass.findAnnotation<IdBase>()
                ?.let { PluginId(idClass, it.displayName) }
        } catch (e: DeclarationException) {
            null
        }
    }

    class Serializer @Inject constructor(
        @Named("PluginClassLoader") private val classLoader: ClassLoader
    ) : ConfigSerializer<PluginId> {
        override fun deserialize(string: String): PluginId {
            val (qualifiedName, displayName) = string.split(':')
            val type = try {
                classLoader.loadClass(qualifiedName).kotlin
            } catch (e: ClassNotFoundException) {
                throw SerializationException()
            }
            return PluginId(type, displayName)
        }

        override fun serialize(obj: PluginId): String {
            return "${obj.qualifiedName}:${obj.displayName}"
        }
    }
}

val KClass<*>.pluginId: PluginId
    get() {
        if (!this.isSubclassOf(Plugin::class))
            throw DeclarationException("Not a plugin subtype: ${this.qualifiedName}")

        PluginId(this)?.let { return it }

        return pluginBases
            .asSequence()
            .mapNotNull { PluginId(it) }
            .firstOrNull()
            ?: throw DeclarationException("No ID base: ${this.qualifiedName}")
    }

val Plugin.id: PluginId
    get() = this::class.pluginId
