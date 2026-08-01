package com.gratitudelogger.data

import com.gratitudelogger.domain.JournalRepository
import com.gratitudelogger.domain.PhotoStorage
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalEntryDao,
    private val photoStorage: PhotoStorage
) : JournalRepository {

    override fun allEntries(): Flow<List<JournalEntry>> = dao.getAllEntries()

    override fun entryById(id: Long): Flow<JournalEntry?> =
        dao.getEntryById(id)

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
        entry.photoPath?.let { photoStorage.deletePhoto(it) }
        dao.delete(entry)
    }
}
