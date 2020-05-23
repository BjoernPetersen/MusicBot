package net.bjoernpetersen.musicbot.spi.plugin

import javax.inject.Inject
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.plugin.InitializationException
import net.bjoernpetersen.musicbot.spi.plugin.management.ProgressUpdater

/**
 * Base interface for plugins. This interface isn't meant to be directly implemented, but to be
 * extended by a specialized interface first.
 *
 * ## Specialized subtypes
 *
 * Your plugin should (directly or indirectly) implement **one** of the following
 * specialized subtypes:
 *
 * - [GenericPlugin]
 * - [PlaybackFactory]
 * - [Provider]
 * - [Suggester]
 *
 * ## Dependencies
 *
 * A plugin can request dependencies (typically other plugins) by marking fields with the
 * [Inject] annotation. Please note that dependency injection in constructors is not possible.
 *
 * ## Lifecycle
 *
 * A plugin goes through several different lifecycle phases due to the way it is integrated in the
 * MusicBot. Those phases are the following, in order:
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
 * [createStateEntries] is also called in this phase, but state entries are never displayed to the
 * user.
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
     * The name should tend to describe **how** it is implemented rather than **what**
     * it's implementing. The user will always know what base the plugin implements and what type it
     * is (Provider, Suggester, ...).
     *
     * This value should be static, i.e. not dependent on config or state.
     *
     * ### Examples
     *
     * - _For a SpotifyPlaybackFactory_: Remote control, Android SDK
     * - _For a VolumeHandler_: Native system master volume, Spotify client volume
     */
    val name: String

    /**
     * A one or two sentence description of the plugin **implementation**.
     */
    val description: String

    /**
     * Create config entries that should be stored in a plain config file and possibly be
     * shown to the user.
     *
     * @param config a Config instance specifically scoped for this method and this plugin
     * @return the created entries which should be shown to the user
     */
    fun createConfigEntries(config: Config): List<Config.Entry<*>>

    /**
     * Create config entries that should be stored as securely as possible and possibly be
     * shown to the user.
     *
     * @param secrets a Config instance specifically scoped for this method and this plugin
     * @return the created entries which should be shown to the user
     */
    fun createSecretEntries(secrets: Config): List<Config.Entry<*>>

    /**
     * Create config entries with which to store the current plugin state.
     *
     * If state entries are deleted between bot runs, the plugin should not lose any
     * manual (user) configuration.
     *
     * @param state a Config instance specifically scoped for this method and this plugin
     */
    fun createStateEntries(state: Config)

    /**
     * Initialize and allocate resources.
     * After this method is called, the plugin is deemed active until [close] is called.
     *
     * @param progressUpdater a writer to tell the user what you're doing
     * @throws InitializationException if any problems occurs during initialization
     */
    @Throws(InitializationException::class)
    suspend fun initialize(progressUpdater: ProgressUpdater)

    /**
     * Close whatever resources have been allocated in [initialize].
     */
    @Throws(Exception::class)
    suspend fun close()
}

/**
 * Marks plugin interfaces which are displayed to the end-user at some point.
 * The plugin should be represented by its [subject] in that case.
 */
interface UserFacing {
    /**
     * The subject of the content provided by this plugin.
     *
     * This will be shown to (client) end users, together with the type of plugin,
     * this should give users an idea of what content they will be getting.
     *
     * Note that the value of this may even change during runtime, especially during configuration.
     *
     * This will not be called before the plugin is [initialized][Plugin.initialize].
     *
     * Some good examples:
     * - For providers: "Spotify" or "YouTube"
     * - For suggesters: "Random MP3s", a playlist name, "Based on `<last played song>`"
     */
    val subject: String
}
