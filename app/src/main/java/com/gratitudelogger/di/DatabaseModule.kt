package com.gratitudelogger.di

import android.content.Context
import androidx.room.Room
import com.gratitudelogger.data.GratitudeDatabase
import com.gratitudelogger.data.JournalEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GratitudeDatabase =
        Room.databaseBuilder(context, GratitudeDatabase::class.java, "gratitude.db").build()

    @Provides
    fun provideJournalEntryDao(database: GratitudeDatabase): JournalEntryDao =
        database.journalEntryDao()
}
