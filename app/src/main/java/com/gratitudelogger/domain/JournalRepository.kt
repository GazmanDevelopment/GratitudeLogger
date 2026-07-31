package com.gratitudelogger.domain

import com.gratitudelogger.data.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface JournalRepository {
    fun entriesForDate(date: LocalDate): Flow<List<JournalEntry>>
    fun entryDatesInMonth(month: YearMonth): Flow<List<LocalDate>>
    suspend fun addEntry(text: String, photoPath: String? = null): JournalEntry
    suspend fun updateEntry(entry: JournalEntry)
    suspend fun deleteEntry(entry: JournalEntry)
}
