package net.bjoernpetersen.musicbot.api.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Paths

private val path = "example${File.separator}file${File.separator}path"

class ConfigSerializerTest {
    @TestFactory
    fun serialization(): List<DynamicTest> {
        return listOf(
            (42 to "42") with IntSerializer,
            (File(path) to path) with FileSerializer,
            (Paths.get(path) to path) with PathSerializer
        ).map { pair ->
            dynamicTest("to ${pair.serializedValue}") {
                assertEquals(pair.serializedValue, pair.serialize())
            }
        }
    }

    @TestFactory
    fun `invalid int deserialization`(): List<DynamicTest> {
        return listOf("", " ", "abc", "1a", "1,2", "1.2", "1.2.3", "1a2")
            .map {
                dynamicTest("for value $it") {
                    assertThrows<SerializationException> {
                        IntSerializer.deserialize("abc")
                    }
                }
            }
    }

    @TestFactory
    fun deserialization(): List<DynamicTest> {
        return listOf(
            (42 to "42") with IntSerializer,
            (File(path) to path) with FileSerializer,
            (Paths.get(path) to path) with PathSerializer
        ).map { pair ->
            dynamicTest("of ${pair.serializedValue}") {
                assertEquals(pair.value, pair.deserialize())
            }
        }
    }
}

private infix fun <T> Pair<T, String>.with(other: ConfigSerializer<T>): SerializationPair<T> =
    SerializationPair(first, second, other)

private data class SerializationPair<T>(
    val value: T,
    val serializedValue: String,
    val serializer: ConfigSerializer<T>
) {
    fun serialize(): String {
        return serializer.serialize(value)
    }

    fun deserialize(): T {
        return serializer.deserialize(serializedValue)
    }
}
