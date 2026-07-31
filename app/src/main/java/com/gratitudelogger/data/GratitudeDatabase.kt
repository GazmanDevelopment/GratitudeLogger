package com.gratitudelogger.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [JournalEntry::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class GratitudeDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao
}
