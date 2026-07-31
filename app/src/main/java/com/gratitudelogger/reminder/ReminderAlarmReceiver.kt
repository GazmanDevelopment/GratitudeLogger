package com.gratitudelogger.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Exact alarms are one-shot, so every fire immediately reschedules tomorrow's alarm -
 * there is no setExactRepeating.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderPreferences: ReminderPreferences

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var reminderNotifier: ReminderNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reminderNotifier.showReminderNotification()
                if (reminderPreferences.currentEnabled()) {
                    val time = reminderPreferences.currentTime()
                    reminderScheduler.scheduleNext(time.hour, time.minute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
