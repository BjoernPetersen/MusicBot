package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.api.auth.Crypto
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.security.SecureRandom

class ByteArraySerializerTest {
    private val serializer: ConfigSerializer<ByteArray> = ByteArraySerializer
    private val random = SecureRandom()

    private fun testSerialization(array: ByteArray) {
        val serialized = serializer.serialize(array)
        val deserialized = serializer.deserialize(serialized)
        assertArrayEquals(array, deserialized)
    }

    @Test
    fun `empty serialization`() {
        testSerialization(ByteArray(0))
    }

    @RepeatedTest(value = 1024)
    fun `random serialization`() {
        val array = Crypto.createRandomBytes(random.nextInt(8192))
        testSerialization(array)
    }
}
