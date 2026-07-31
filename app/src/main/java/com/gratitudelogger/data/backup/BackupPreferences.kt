package com.gratitudelogger.data.backup

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.gratitudelogger.domain.backup.LastBackupInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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

    suspend fun recordBackup(accountLabel: String, timestamp: Instant) {
        context.backupDataStore.edit { prefs ->
            prefs[Keys.ACCOUNT_LABEL] = accountLabel
            prefs[Keys.LAST_BACKUP_MILLIS] = timestamp.toEpochMilli()
        }
    }
}
