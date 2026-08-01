package com.gratitudelogger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gratitudelogger.reminder.BackupCheckWorker
import com.gratitudelogger.reminder.BackupReminderNotifier
import com.gratitudelogger.reminder.ReminderNotifier
import com.gratitudelogger.reminder.ReminderPreferences
import com.gratitudelogger.reminder.ReminderScheduler
import com.gratitudelogger.security.AppLockManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class GratitudeLoggerApp : Application(), Configuration.Provider {

    // Injected (not just referenced) so Hilt constructs it - and registers its
    // ProcessLifecycleOwner observer - during app startup rather than on first UI use.
    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var reminderPreferences: ReminderPreferences

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            ReminderNotifier.CHANNEL_ID,
            "Daily Gratitude Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val backupChannel = NotificationChannel(
            BackupReminderNotifier.CHANNEL_ID,
            "Backup Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(channel)
            createNotificationChannel(backupChannel)
        }

        // The reminder defaults to enabled from first install (ReminderPreferences.enabled),
        // but a preference default alone doesn't schedule anything - this mirrors
        // BootReceiver's own restore-on-reboot logic so a fresh install actually gets a
        // scheduled alarm without the user ever having to open Settings. Idempotent and
        // cheap to repeat on every process start (same as re-scheduling after a time change).
        CoroutineScope(Dispatchers.IO).launch {
            if (reminderPreferences.currentEnabled()) {
                val time = reminderPreferences.currentTime()
                reminderScheduler.scheduleNext(time.hour, time.minute)
            }
        }

        // Runs unconditionally (KEEP makes repeat calls idempotent) - the worker itself checks
        // BackupPreferences.reminderEnabled and no-ops if it's off, so toggling the Settings
        // switch never needs to enqueue/cancel any work. Periodic, not exact-timed, so
        // WorkManager (which persists this registration across reboots on its own) is a better
        // fit here than the AlarmManager+BootReceiver pattern the daily reminder uses.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "backup_reminder_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BackupCheckWorker>(1, TimeUnit.DAYS).build()
        )
    }
}
