package com.gratitudelogger.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore by preferencesDataStore(name = "security_prefs")

@Singleton
class SecurityPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_ITERATIONS = intPreferencesKey("pin_iterations")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    val isPinSet: Flow<Boolean> = context.securityDataStore.data.map { it[Keys.PIN_HASH] != null }

    val biometricEnabled: Flow<Boolean> = context.securityDataStore.data
        .map { it[Keys.BIOMETRIC_ENABLED] ?: false }

    suspend fun setPin(pin: String) {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash(pin, salt)
        context.securityDataStore.edit { prefs ->
            prefs[Keys.PIN_HASH] = hash
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.PIN_ITERATIONS] = PinHasher.DEFAULT_ITERATIONS
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.securityDataStore.data.first()
        val storedHash = prefs[Keys.PIN_HASH] ?: return false
        val salt = prefs[Keys.PIN_SALT] ?: return false
        val iterations = prefs[Keys.PIN_ITERATIONS] ?: PinHasher.DEFAULT_ITERATIONS
        return PinHasher.hash(pin, salt, iterations) == storedHash
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.securityDataStore.edit { prefs -> prefs[Keys.BIOMETRIC_ENABLED] = enabled }
    }
}
