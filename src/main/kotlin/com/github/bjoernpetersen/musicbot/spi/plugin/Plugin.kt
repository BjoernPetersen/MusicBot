package com.github.bjoernpetersen.musicbot.spi.plugin

import com.github.bjoernpetersen.musicbot.api.config.Config
import com.github.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import java.io.IOException
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

/**
 * Base interface for plugins. This interface isn't meant to be directly implemented, but to be
 * extended by a specialized interface first.
 *
 * Every plugin MUST be annotated with [Bases] and [IdBase].
 *
 * ## Dependencies
 *
 * A plugin can request dependencies (typically other plugins) by marking fields with the
 * [Inject] annotation. Please note that dependency injection in constructors is not possible.
 *
 * ## Lifecycle
 *
 * A plugin goes through several different lifecycle phases due to the way it is integrated in the
 * MusicBot.
 *
 * ### Creation
 *
 * The plugin is loaded from an external Jar-File and instantiated using a no-args constructor.
 * In this phase, no actual work should be done by the plugin and no resources should be allocated.
 *
 * ### Dependency injection
 *
 * In a second, **separate** step, the dependencies requested by the plugin are resolved
 * and injected. There is no notification for the plugin about this.
 *
 * ### Configuration
 *
 * The plugin is asked to provide its configuration entries by [createConfigEntries] and
 * [createSecretEntries]. The returned entries are shown to the user and their value may change
 * any number of times during this phase.
 *
 * ### Initialization
 *
 * The [initialize] method is called after configuration is done and the plugin may allocate
 * additional resources for the time it is running.
 *
 * ### Destruction
 *
 * The [close] method is called and all resources should be released.
 * The plugin instance will never be used again at this point.
 *
 */
interface Plugin {

    /**
     * An arbitrary plugin name. Keep it short, but descriptive.
     *
     * The name will never be shown without the user knowing the specialized plugin interface
     * this plugin implements (Provider, Suggester, ...), so please don't include that part in the
     * name.
     *
     * This value should be static, i.e. not dependent on config or state.
     */
    val name: String

    /**
     * A one or two sentence description of the plugin.
     */
    val description: String

    fun createConfigEntries(config: Config): List<Config.Entry<*>>
    fun createSecretEntries(secrets: Config): List<Config.Entry<*>>
    fun createStateEntries(state: Config)

    @Throws(InitializationException::class)
    fun initialize(initStateWriter: InitStateWriter)

    @Throws(IOException::class)
    fun close()
}

interface UserFacing {
    /**
     * The subject of the content provided by this plugin.
     *
     * This will be shown to end users, together with the type of plugin, this should give users
     * an idea of they will be getting.
     *
     * Some good examples:
     * - For providers: "Spotify" or "YouTube"
     * - For suggesters: "Random MP3s", a playlist name, "Based on last played song"
     */
    val subject: String
}

/**
 * Marks plugin types that are implicitly required, even though they aren't requested as a
 * dependency.
 */
interface Active

/**
 * An exception during plugin initialization.
 */
open class InitializationException : Exception {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * An exception thrown by Plugins if they are misconfigured.
 */
class ConfigurationException : InitializationException {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

private fun KClass<*>.pluginSpecification(): KClass<out Plugin>? {
    val specs = mutableListOf<KClass<out Plugin>>()
    sequenceOf(GenericPlugin::class, PlaybackFactory::class, Provider::class, Suggester::class)
        .filter { this.isSubclassOf(it) }
        .forEach { specs.add(it) }
    return if (specs.size == 1) specs.first()
    else null
}

val Plugin.isValid: Boolean
    get() {
        val id = try {
            id
        } catch (e: MissingIdBaseException) {
            return false
        }

        val bases = try {
            bases
        } catch (e: MissingBasesException) {
            return false
        }

        if (id !in bases) {
            return false
        }

        val specs = mutableSetOf<KClass<out Plugin>>()

        val selfSpec = this::class.pluginSpecification() ?: return false
        specs.add(selfSpec)

        for (base in bases) {
            val spec = base.pluginSpecification() ?: return false
            specs.add(spec)

            if (!base.isSuperclassOf(this::class)) return false
        }

        return specs.size == 1
    }
