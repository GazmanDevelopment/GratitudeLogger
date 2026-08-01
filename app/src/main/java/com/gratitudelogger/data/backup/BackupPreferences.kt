package com.gratitudelogger.data.backup

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.gratitudelogger.domain.backup.BackupProviderType
import com.gratitudelogger.domain.backup.LastBackupInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backupDataStore by preferencesDataStore(name = "backup_prefs")

@Singleton
class BackupPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val ACCOUNT_LABEL = stringPreferencesKey("account_label")
        val LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
        val REMINDER_ENABLED = booleanPreferencesKey("backup_reminder_enabled")
        val REMINDER_INTERVAL_DAYS = intPreferencesKey("backup_reminder_interval_days")
        val DROPBOX_REFRESH_TOKEN = stringPreferencesKey("dropbox_refresh_token")
        val SELECTED_PROVIDER = stringPreferencesKey("selected_backup_provider")
    }

    val lastBackupInfo: Flow<LastBackupInfo?> = context.backupDataStore.data.map { prefs ->
        val label = prefs[Keys.ACCOUNT_LABEL]
        val millis = prefs[Keys.LAST_BACKUP_MILLIS]
        if (label != null && millis != null) {
            LastBackupInfo(accountLabel = label, timestamp = Instant.ofEpochMilli(millis))
        } else {
            null
        }
    }

    val reminderEnabled: Flow<Boolean> =
        context.backupDataStore.data.map { it[Keys.REMINDER_ENABLED] ?: true }

    val reminderIntervalDays: Flow<Int> =
        context.backupDataStore.data.map { it[Keys.REMINDER_INTERVAL_DAYS] ?: DEFAULT_REMINDER_INTERVAL_DAYS }

    val dropboxRefreshToken: Flow<String?> =
        context.backupDataStore.data.map { it[Keys.DROPBOX_REFRESH_TOKEN] }

    val selectedProvider: Flow<BackupProviderType> = context.backupDataStore.data.map { prefs ->
        prefs[Keys.SELECTED_PROVIDER]?.let { name ->
            BackupProviderType.entries.find { it.name == name }
        } ?: BackupProviderType.GOOGLE_DRIVE
    }

    suspend fun currentReminderEnabled(): Boolean = reminderEnabled.first()

    suspend fun currentReminderIntervalDays(): Int = reminderIntervalDays.first()

    suspend fun currentDropboxRefreshToken(): String? = dropboxRefreshToken.first()

    suspend fun currentSelectedProvider(): BackupProviderType = selectedProvider.first()

    suspend fun recordBackup(accountLabel: String, timestamp: Instant) {
        context.backupDataStore.edit { prefs ->
            prefs[Keys.ACCOUNT_LABEL] = accountLabel
            prefs[Keys.LAST_BACKUP_MILLIS] = timestamp.toEpochMilli()
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderIntervalDays(days: Int) {
        context.backupDataStore.edit { it[Keys.REMINDER_INTERVAL_DAYS] = days }
    }

    suspend fun setDropboxRefreshToken(token: String?) {
        context.backupDataStore.edit { prefs ->
            if (token != null) prefs[Keys.DROPBOX_REFRESH_TOKEN] = token else prefs.remove(Keys.DROPBOX_REFRESH_TOKEN)
        }
    }

    suspend fun setSelectedProvider(provider: BackupProviderType) {
        context.backupDataStore.edit { it[Keys.SELECTED_PROVIDER] = provider.name }
    }

    companion object {
        const val DEFAULT_REMINDER_INTERVAL_DAYS = 14
    }
}
