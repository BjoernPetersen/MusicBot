package net.bjoernpetersen.musicbot.api.auth

import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.util.Base64
import java.util.Random

/**
 * Provides basic crypto features.
 */
object Crypto {
    private const val SIGNATURE_KEY_SIZE = 4096
    private val random: Random = SecureRandom()

    /**
     * Creates a hash string from a clear-text [password].
     */
    fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    /**
     * Creates a key with the specified [length] which can be used for cryptographic signatures.
     */
    fun createRandomBytes(length: Int = SIGNATURE_KEY_SIZE): ByteArray {
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes
    }
}
