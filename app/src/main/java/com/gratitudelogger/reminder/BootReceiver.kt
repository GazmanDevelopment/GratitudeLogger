package com.gratitudelogger.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Exact alarms are cleared on reboot, so the schedule must be restored here if enabled. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderPreferences: ReminderPreferences

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
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
