package com.gratitudelogger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface JournalEntryDao {
    @Insert
    suspend fun insert(entry: JournalEntry): Long

    @Update
    suspend fun update(entry: JournalEntry)

    @Delete
    suspend fun delete(entry: JournalEntry)

    @Query("SELECT * FROM journal_entries WHERE entryDate = :date ORDER BY createdAt DESC")
    fun getEntriesForDate(date: LocalDate): Flow<List<JournalEntry>>

    @Query("SELECT DISTINCT entryDate FROM journal_entries WHERE entryDate BETWEEN :start AND :end")
    fun getEntryDatesInRange(start: LocalDate, end: LocalDate): Flow<List<LocalDate>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<JournalEntry?>

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun countEntries(): Int
}
