package com.gratitudelogger.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gratitudelogger.data.backup.BackupPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit

@HiltWorker
class BackupCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupPreferences: BackupPreferences,
    private val notifier: BackupReminderNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (backupPreferences.currentReminderEnabled()) {
            val lastBackup = backupPreferences.lastBackupInfo.first()
            val intervalDays = backupPreferences.currentReminderIntervalDays()
            val overdue = lastBackup == null ||
                ChronoUnit.DAYS.between(lastBackup.timestamp, Instant.now()) >= intervalDays
            if (overdue) notifier.showBackupReminderNotification()
        }
        return Result.success()
    }
}
