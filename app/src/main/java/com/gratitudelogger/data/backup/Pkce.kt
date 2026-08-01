package com.gratitudelogger.data.backup

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * OAuth2 PKCE (RFC 7636) helpers shared by every browser-auth backup provider
 * (DropboxBackupProvider, OneDriveBackupProvider) - the algorithm is identical
 * regardless of which provider's authorize/token endpoints are being used.
 */
internal object Pkce {
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun codeChallengeFor(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
