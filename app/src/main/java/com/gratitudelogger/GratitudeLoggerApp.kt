package com.gratitudelogger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.gratitudelogger.reminder.ReminderNotifier
import com.gratitudelogger.reminder.ReminderPreferences
import com.gratitudelogger.reminder.ReminderScheduler
import com.gratitudelogger.security.AppLockManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GratitudeLoggerApp : Application() {

    // Injected (not just referenced) so Hilt constructs it - and registers its
    // ProcessLifecycleOwner observer - during app startup rather than on first UI use.
    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var reminderPreferences: ReminderPreferences

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            ReminderNotifier.CHANNEL_ID,
            "Daily Gratitude Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

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
    }
}
