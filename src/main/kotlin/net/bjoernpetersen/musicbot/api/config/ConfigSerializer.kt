package net.bjoernpetersen.musicbot.api.config

import java.io.File

interface ConfigSerializer<T> {
    /**
     * Serializes the given object to a string.
     *
     * This operation should not fail.
     */
    fun serialize(obj: T): String

    /**
     * Deserializes the given string.
     *
     * @throws SerializationException if the string can't be deserialized
     */
    @Throws(SerializationException::class)
    fun deserialize(string: String): T
}

class SerializationException : Exception()

object IntSerializer : ConfigSerializer<Int> {
    override fun serialize(obj: Int): String = obj.toString()
    @Throws(SerializationException::class)
    override fun deserialize(string: String): Int = string.toIntOrNull()
        ?: throw SerializationException()
}

/**
 * Serializer for files. Will not check for existence on deserialization.
 */
object FileSerializer : ConfigSerializer<File> {

    override fun serialize(obj: File): String {
        return obj.path
    }

    override fun deserialize(string: String): File {
        return File(string)
    }
}
