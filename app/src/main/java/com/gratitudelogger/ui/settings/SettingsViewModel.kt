package com.gratitudelogger.ui.settings

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gratitudelogger.data.backup.BackupPreferences
import com.gratitudelogger.reminder.ReminderPreferences
import com.gratitudelogger.reminder.ReminderScheduler
import com.gratitudelogger.reminder.ReminderTime
import com.gratitudelogger.security.SecurityPreferences
import com.gratitudelogger.theme.AppTheme
import com.gratitudelogger.theme.EntryOrder
import com.gratitudelogger.theme.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderPreferences: ReminderPreferences,
    private val reminderScheduler: ReminderScheduler,
    private val securityPreferences: SecurityPreferences,
    private val themePreferences: ThemePreferences,
    private val backupPreferences: BackupPreferences,
    @ApplicationContext context: Context
) : ViewModel() {

    private val biometricAvailability: Int =
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

    val isBiometricAvailable: Boolean = biometricAvailability == BiometricManager.BIOMETRIC_SUCCESS

    val biometricUnavailableReason: String? = when (biometricAvailability) {
        BiometricManager.BIOMETRIC_SUCCESS -> null
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "This device doesn't support biometric unlock"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is currently unavailable"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
            "No fingerprint or face is set up on this device - add one in your device settings"
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
            "A security update is required before biometric unlock can be used"
        else -> "Biometric unlock is unavailable on this device"
    }

    val reminderEnabled: StateFlow<Boolean> = reminderPreferences.enabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val reminderTime: StateFlow<ReminderTime> = reminderPreferences.time
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ReminderTime(ReminderPreferences.DEFAULT_HOUR, ReminderPreferences.DEFAULT_MINUTE)
        )

    val biometricEnabled: StateFlow<Boolean> = securityPreferences.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val selectedTheme: StateFlow<AppTheme> = themePreferences.selectedTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.SUNSET_GOLD)

    val selectedEntryOrder: StateFlow<EntryOrder> = themePreferences.selectedEntryOrder
        .stateIn(viewModelScope, SharingStarted.Eagerly, EntryOrder.NEWEST_FIRST)

    val backupReminderEnabled: StateFlow<Boolean> = backupPreferences.reminderEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val backupReminderIntervalDays: StateFlow<Int> = backupPreferences.reminderIntervalDays
        .stateIn(viewModelScope, SharingStarted.Eagerly, BackupPreferences.DEFAULT_REMINDER_INTERVAL_DAYS)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { themePreferences.setTheme(theme) }
    }

    fun setEntryOrder(order: EntryOrder) {
        viewModelScope.launch { themePreferences.setEntryOrder(order) }
    }

    fun canScheduleExactAlarms(): Boolean = reminderScheduler.canScheduleExactAlarms()

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderPreferences.setEnabled(enabled)
            if (enabled) {
                val time = reminderPreferences.currentTime()
                reminderScheduler.scheduleNext(time.hour, time.minute)
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            reminderPreferences.setTime(hour, minute)
            if (reminderPreferences.currentEnabled()) {
                reminderScheduler.scheduleNext(hour, minute)
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { securityPreferences.setBiometricEnabled(enabled) }
    }

    fun setBackupReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { backupPreferences.setReminderEnabled(enabled) }
    }

    fun setBackupReminderIntervalDays(days: Int) {
        viewModelScope.launch { backupPreferences.setReminderIntervalDays(days.coerceAtLeast(1)) }
    }
}
