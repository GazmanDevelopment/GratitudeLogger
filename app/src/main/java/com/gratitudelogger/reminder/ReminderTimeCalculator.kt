package com.gratitudelogger.reminder

import java.time.ZonedDateTime

/**
 * Pure "next occurrence" math, kept separate from AlarmManager so it can be unit tested
 * without an Android framework dependency. Using ZonedDateTime (not epoch millis) means
 * DST transitions are handled correctly: adding a day preserves wall-clock time and lets
 * the zone rules resolve the correct instant for that date.
 */
object ReminderTimeCalculator {
    fun nextTriggerMillis(now: ZonedDateTime, hour: Int, minute: Int): Long {
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.toInstant().toEpochMilli()
    }
}
