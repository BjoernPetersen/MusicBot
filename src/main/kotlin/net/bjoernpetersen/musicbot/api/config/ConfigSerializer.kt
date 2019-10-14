package net.bjoernpetersen.musicbot.api.config

import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Defines serialization and deserialization for config entry values.
 *
 * @param T the value type
 */
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

/**
 * Thrown if a value can't be deserialized.
 */
class SerializationException : Exception()

/**
 * Serializer for integer values.
 */
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

/**
 * Serializer for paths. Will not check for existence on deserialization.
 */
object PathSerializer : ConfigSerializer<Path> {

    override fun serialize(obj: Path): String = obj.toString()

    override fun deserialize(string: String): Path = try {
        Paths.get(string)
    } catch (e: InvalidPathException) {
        throw SerializationException()
    }
}

private class ListSerializer<T>(
    private val itemSerializer: ConfigSerializer<T>,
    private val separator: String
) : ConfigSerializer<List<T>> {
    override fun serialize(obj: List<T>): String {
        return obj.joinToString(separator) { itemSerializer.serialize(it) }
    }

    override fun deserialize(string: String): List<T> {
        return string.split(separator)
            .map { itemSerializer.deserialize(it) }
    }
}

/**
 * A serializer for a list of strings.
 */
object StringListSerializer : ConfigSerializer<List<String>> by ListSerializer(
    serialization {
        serialize { it }
        deserialize { it }
    },
    ","
)

/**
 * Creates a List serializer which delegates the serialization of the items to the serializer this
 * method was called on.
 *
 * @param separator the separator between list items in the serialized form
 * @return a config serializer for lists
 */
fun <T> ConfigSerializer<T>.listSerializer(separator: String = ","): ConfigSerializer<List<T>> {
    return ListSerializer(this, separator)
}

private class SetSerializer<T>(
    private val itemSerializer: ConfigSerializer<T>,
    private val separator: String
) : ConfigSerializer<Set<T>> {
    override fun serialize(obj: Set<T>): String {
        return obj.joinToString(separator) { itemSerializer.serialize(it) }
    }

    override fun deserialize(string: String): Set<T> {
        return string.splitToSequence(separator)
            .map { itemSerializer.deserialize(it) }
            .toSet()
    }
}

/**
 * A serializer for a set of strings.
 */
object StringSetSerializer : ConfigSerializer<Set<String>> by SetSerializer(
    serialization {
        serialize { it }
        deserialize { it }
    },
    ","
)

/**
 * Creates a Set serializer which delegates the serialization of the items to the serializer this
 * method was called on.
 *
 * @param separator the separator between set items in the serialized form
 * @return a config serializer for sets
 */
fun <T> ConfigSerializer<T>.setSerializer(separator: String = ","): ConfigSerializer<Set<T>> {
    return SetSerializer(this, separator)
}
