package net.bjoernpetersen.musicbot.api.config

import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.config.Config.BooleanEntry
import net.bjoernpetersen.musicbot.api.config.Config.SerializedEntry
import net.bjoernpetersen.musicbot.spi.config.ConfigChecker
import net.bjoernpetersen.musicbot.spi.config.ConfigStorageAdapter

/**
 * A configuration view for a specific config scope.
 * There are many instances of Config per bot-instance for various scopes.
 *
 * The actual config location is determined by the [storage adapter][adapter].
 *
 * At the root, every config entry is stored as a string. You can create entries for other types
 * by providing a serializer/deserializer to [SerializedEntry] though.
 * For the common case of boolean values, [BooleanEntry] is already predefined.
 */
class Config internal constructor(
    private val adapter: ConfigStorageAdapter,
    private val scope: ConfigScope) {

    private val logger = KotlinLogging.logger {}
    private val entries: MutableMap<String, String> = adapter.load(scope).toMutableMap()

    private fun getValue(key: String): String? {
        return entries[key]?.let {
            if (it.isBlank()) null
            else it
        }
    }

    private fun setValue(key: String, value: String?) {
        val old = getValue(key)
        if (old != value) {
            logger.debug {
                "Config[$scope] entry '$key' changed"
            }
            if (value.isNullOrBlank()) {
                entries.remove(key)
            } else {
                entries[key] = value
            }
            adapter.store(scope, entries)
        }
    }

    /**
     * Base class for config entries.
     *
     * @param key the unique (in scope) entry key
     * @param description a description of what the entry does
     * @param uiNode a visual representation of the config entry, not needed for state
     */
    abstract inner class Entry<T> internal constructor(
        val key: String,
        val description: String,
        val uiNode: UiNode<in T>?) {

        abstract fun getWithoutDefault(): T?
        abstract fun get(): T?
        abstract fun set(value: T?)
        abstract fun checkError(): String?
    }

    /**
     * A string config entry.
     *
     * @param key the unique (in scope) entry key
     * @param description a description of what the entry does
     * @param configChecker a validator for entry values
     * @param uiNode a visual representation of the config entry, not needed for state
     * @param default a default value
     */
    inner class StringEntry @JvmOverloads constructor(
        key: String,
        description: String,
        private val configChecker: ConfigChecker<in String>,
        uiNode: UiNode<in String>? = null,
        val default: String? = null) : Entry<String>(key, description, uiNode) {

        override fun getWithoutDefault(): String? {
            return getValue(key)
        }

        /**
         * Gets the value of this entry, or the default value if there is none.
         */
        override fun get(): String? {
            return getWithoutDefault() ?: default
        }

        /**
         * Sets the value for this entry. Note that blank values are handled like `null`.
         */
        override fun set(value: String?) {
            setValue(key, value)
        }

        override fun checkError(): String? = configChecker(get())
    }

    /**
     * A config entry accepting a non-string type.
     *
     * @param key the unique (in scope) entry key
     * @param description a description of what the entry does
     * @param serializer a config serializer to convert from/to string
     * @param configChecker a validator for entry values
     * @param uiNode a visual representation of the config entry, not needed for state
     * @param default a default value
     */
    open inner class SerializedEntry<T> @JvmOverloads constructor(
        key: String,
        description: String,
        private val serializer: ConfigSerializer<T>,
        private val configChecker: ConfigChecker<in T>,
        uiNode: UiNode<in T>? = null,
        val default: T? = null) : Entry<T>(key, description, uiNode) {

        override fun getWithoutDefault(): T? = try {
            getValue(key)?.let { serializer.deserialize(it) }
        } catch (e: SerializationException) {
            logger.error(e) { "Deserialization failure" }
            setValue(key, null)
            null
        }

        override fun get(): T? = getWithoutDefault() ?: default

        override fun set(value: T?) {
            setValue(key, value?.let { serializer.serialize(it) })
        }

        override fun checkError(): String? = configChecker(get())
    }

    /**
     * A boolean config entry.
     *
     * @param key the unique (in scope) entry key
     * @param description a description of what the entry does
     * @param default a default value
     */
    inner class BooleanEntry(
        key: String,
        description: String,
        default: Boolean) :
        SerializedEntry<Boolean>(key, description, BooleanSerializer, { null }, CheckBox, default) {

        override fun get(): Boolean = super.get()!!
    }
}

private object BooleanSerializer : ConfigSerializer<Boolean> {
    override fun serialize(obj: Boolean): String = obj.toString()
    override fun deserialize(string: String): Boolean = string.toBoolean()
}
