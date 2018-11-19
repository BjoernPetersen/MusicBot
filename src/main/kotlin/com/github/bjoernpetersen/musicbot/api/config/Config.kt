package com.github.bjoernpetersen.musicbot.api.config

import com.github.bjoernpetersen.musicbot.spi.config.ConfigChecker
import com.github.bjoernpetersen.musicbot.spi.config.ConfigStorageAdapter
import mu.KotlinLogging

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

    // TODO listeners

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

    abstract inner class Entry<T> internal constructor(
        val key: String,
        val description: String,
        val uiNode: UiNode<in T>?) {

        abstract fun getWithoutDefault(): T?
        abstract fun get(): T?
        abstract fun set(value: T?)
        abstract fun checkError(): String?
    }

    inner class StringEntry @JvmOverloads constructor(
        key: String,
        description: String,
        private val configChecker: ConfigChecker<String>,
        uiNode: UiNode<in String>? = null,
        val default: String? = null) : Entry<String>(key, description, uiNode) {

        override fun getWithoutDefault(): String? {
            return getValue(key)
        }

        override fun get(): String? {
            return getWithoutDefault() ?: default
        }

        override fun set(value: String?) {
            setValue(key, value)
        }

        override fun checkError(): String? = configChecker(get())
    }

    open inner class SerializedEntry<T> @JvmOverloads constructor(
        key: String,
        description: String,
        private val serializer: ConfigSerializer<T>,
        private val configChecker: ConfigChecker<T>,
        uiNode: UiNode<in T>? = null,
        private val default: T? = null) : Entry<T>(key, description, uiNode) {

        override fun getWithoutDefault(): T? = try {
            getValue(key)?.let { serializer.deserialize(it) }
        } catch (e: SerializationException) {
            logger.error(e) { "Deserialization failure" }
            setValue(key, null)
            null
        }

        @Throws(SerializationException::class)
        override fun get(): T? = getWithoutDefault() ?: default

        override fun set(value: T?) {
            setValue(key, value?.let { serializer.serialize(it) })
        }

        override fun checkError(): String? = configChecker(getWithoutDefault())
    }

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
