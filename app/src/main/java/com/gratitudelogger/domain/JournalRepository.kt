package com.gratitudelogger.domain

import com.gratitudelogger.data.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun allEntries(): Flow<List<JournalEntry>>
    fun entryById(id: Long): Flow<JournalEntry?>
    suspend fun addEntry(text: String, photoPath: String? = null): JournalEntry
    suspend fun updateEntry(entry: JournalEntry)
    suspend fun deleteEntry(entry: JournalEntry)
}
