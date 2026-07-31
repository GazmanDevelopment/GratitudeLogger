package com.gratitudelogger.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN is only ever verified, never decrypted, so a salted PBKDF2 hash is sufficient -
 * no need for reversible encryption (EncryptedSharedPreferences/Keystore).
 */
object PinHasher {
    const val DEFAULT_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(pin: String, salt: String, iterations: Int = DEFAULT_ITERATIONS): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
