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
    private val encoder = Base64.getEncoder()

    /**
     * Creates a hash string from a clear-text [password].
     */
    fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    /**
     * Creates a key which can be used for HMAC512 signatures.
     */
    fun createSignatureKey(): ByteArray {
        val bytes = ByteArray(SIGNATURE_KEY_SIZE)
        random.nextBytes(bytes)
        return bytes
    }
}
