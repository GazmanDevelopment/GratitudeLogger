package com.gratitudelogger.reminder

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore by preferencesDataStore(name = "reminder_prefs")

data class ReminderTime(val hour: Int, val minute: Int)

@Singleton
class ReminderPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val ENABLED = booleanPreferencesKey("reminder_enabled")
        val HOUR = intPreferencesKey("reminder_hour")
        val MINUTE = intPreferencesKey("reminder_minute")
    }

    // Defaults to true so the reminder is on from first install, not something the user has
    // to discover and opt into in Settings.
    val enabled: Flow<Boolean> = context.reminderDataStore.data.map { it[Keys.ENABLED] ?: true }

    val time: Flow<ReminderTime> = context.reminderDataStore.data.map {
        ReminderTime(
            hour = it[Keys.HOUR] ?: DEFAULT_HOUR,
            minute = it[Keys.MINUTE] ?: DEFAULT_MINUTE
        )
    }

    suspend fun currentTime(): ReminderTime = time.first()

    suspend fun currentEnabled(): Boolean = enabled.first()

    suspend fun setEnabled(value: Boolean) {
        context.reminderDataStore.edit { it[Keys.ENABLED] = value }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        context.reminderDataStore.edit {
            it[Keys.HOUR] = hour
            it[Keys.MINUTE] = minute
        }
    }

    companion object {
        const val DEFAULT_HOUR = 20
        const val DEFAULT_MINUTE = 0
    }
}
