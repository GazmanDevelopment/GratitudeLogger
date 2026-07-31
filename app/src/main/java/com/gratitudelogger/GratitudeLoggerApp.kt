package com.gratitudelogger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.gratitudelogger.reminder.ReminderNotifier
import com.gratitudelogger.security.AppLockManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GratitudeLoggerApp : Application() {

    // Injected (not just referenced) so Hilt constructs it - and registers its
    // ProcessLifecycleOwner observer - during app startup rather than on first UI use.
    @Inject
    lateinit var appLockManager: AppLockManager

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            ReminderNotifier.CHANNEL_ID,
            "Daily Gratitude Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
