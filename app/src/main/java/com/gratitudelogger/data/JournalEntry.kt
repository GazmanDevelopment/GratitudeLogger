package com.gratitudelogger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

enum class SyncState { LOCAL_ONLY, PENDING_UPLOAD, SYNCED }

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryDate: LocalDate,
    val text: String,
    val photoPath: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState = SyncState.LOCAL_ONLY
)
