package net.bjoernpetersen.musicbot.api.auth

import java.security.SecureRandom
import java.util.Random
import org.mindrot.jbcrypt.BCrypt

/**
 * Provides basic crypto features.
 */
object Crypto {
    private const val SIGNATURE_KEY_SIZE = 128
    private val random: Random = SecureRandom()

    /**
     * Creates a hash string from a clear-text [password].
     */
    fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    /**
     * Creates a key with the specified [length] which can be used for cryptographic signatures.
     *
     * @param length the key size in bytes
     */
    fun createRandomBytes(length: Int = SIGNATURE_KEY_SIZE): ByteArray {
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes
    }
}
