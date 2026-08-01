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

// GoogleDriveBackupProvider and DropboxBackupProvider are both plain @Singleton @Inject
// constructor-injected classes - BackupViewModel injects them directly by concrete type and
// picks the active one at call time from BackupPreferences.selectedProvider, since there are
// now two providers rather than one fixed BackupProvider binding.
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
