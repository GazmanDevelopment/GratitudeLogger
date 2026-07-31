package com.gratitudelogger.di

import com.gratitudelogger.data.JournalRepositoryImpl
import com.gratitudelogger.data.PhotoStorageImpl
import com.gratitudelogger.domain.JournalRepository
import com.gratitudelogger.domain.PhotoStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository

    @Binds
    @Singleton
    abstract fun bindPhotoStorage(impl: PhotoStorageImpl): PhotoStorage
}
