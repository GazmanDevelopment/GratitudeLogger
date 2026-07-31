package com.gratitudelogger.data

import com.gratitudelogger.domain.JournalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalEntryDao
) : JournalRepository {

    override fun entriesForDate(date: LocalDate): Flow<List<JournalEntry>> =
        dao.getEntriesForDate(date)

    override fun entryDatesInMonth(month: YearMonth): Flow<List<LocalDate>> =
        dao.getEntryDatesInRange(month.atDay(1), month.atEndOfMonth())

    override suspend fun addEntry(text: String, photoPath: String?): JournalEntry {
        val now = Instant.now()
        val entry = JournalEntry(
            entryDate = LocalDate.now(),
            text = text,
            photoPath = photoPath,
            createdAt = now,
            updatedAt = now
        )
        val id = dao.insert(entry)
        return entry.copy(id = id)
    }

    override suspend fun updateEntry(entry: JournalEntry) {
        dao.update(entry.copy(updatedAt = Instant.now()))
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        dao.delete(entry)
    }
}
