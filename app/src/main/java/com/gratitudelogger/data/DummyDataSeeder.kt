package com.gratitudelogger.data

import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Seeds a handful of sample entries spanning the current and previous month so the
 * calendar's dot indicators and month navigation can be visually validated before
 * real entry creation (M2) exists. Safe to delete once M2 lands.
 */
class DummyDataSeeder @Inject constructor(
    private val dao: JournalEntryDao
) {
    suspend fun seedIfEmpty() {
        if (dao.countEntries() > 0) return

        val today = LocalDate.now()
        val sampleOffsetsInDays = listOf(0, -1, -3, -8, -15, -32, -40, -50)
        sampleOffsetsInDays.forEach { offset ->
            val date = today.plusDays(offset.toLong())
            val now = Instant.now()
            dao.insert(
                JournalEntry(
                    entryDate = date,
                    text = "Sample gratitude entry for $date",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}
