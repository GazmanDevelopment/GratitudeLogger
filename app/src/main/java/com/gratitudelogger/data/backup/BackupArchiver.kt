package com.gratitudelogger.data.backup

import android.content.Context
import android.content.Intent
import com.gratitudelogger.MainActivity
import com.gratitudelogger.data.GratitudeDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val DB_ENTRY_NAME = "gratitude.db"
private const val PHOTOS_ENTRY_PREFIX = "photos/"

@Singleton
class BackupArchiver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: GratitudeDatabase
) {
    /** Zips the current local state (checkpointed DB + all photos) into a fresh temp file. */
    suspend fun createBackupZip(): File = withContext(Dispatchers.IO) {
        // Room's WAL journal can leave recent writes sitting only in -wal; checkpoint first
        // so the .db file alone is a complete, consistent snapshot (no need to also carry
        // the -wal/-shm sidecars into the backup).
        database.query("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }

        val zipFile = File(context.cacheDir, "gratitude-backup-upload.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val dbFile = context.getDatabasePath("gratitude.db")
            if (dbFile.exists()) {
                zos.putNextEntry(ZipEntry(DB_ENTRY_NAME))
                FileInputStream(dbFile).use { it.copyTo(zos) }
                zos.closeEntry()
            }

            val photosDir = File(context.filesDir, "photos")
            photosDir.listFiles()?.forEach { photo ->
                if (photo.isFile) {
                    zos.putNextEntry(ZipEntry(PHOTOS_ENTRY_PREFIX + photo.name))
                    FileInputStream(photo).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        zipFile
    }

    /**
     * Extracts a downloaded backup zip over the local DB/photos, then force-restarts the
     * process so Hilt/Room/DataStore all reinitialize from the restored files rather than
     * trying to keep already-open singletons consistent with files swapped out underneath
     * them.
     */
    suspend fun restoreFromZip(zipBytes: ByteArray): Nothing = withContext(Dispatchers.IO) {
        database.close()

        val dbFile = context.getDatabasePath("gratitude.db")
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()

        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        photosDir.listFiles()?.forEach { it.delete() }

        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val target = when {
                    entry.name == DB_ENTRY_NAME -> dbFile
                    entry.name.startsWith(PHOTOS_ENTRY_PREFIX) ->
                        File(photosDir, entry.name.removePrefix(PHOTOS_ENTRY_PREFIX))
                    else -> null
                }
                if (target != null) {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        restartApp()
    }

    private fun restartApp(): Nothing {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
        throw IllegalStateException("unreachable")
    }
}
