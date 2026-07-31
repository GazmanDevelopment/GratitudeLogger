package com.gratitudelogger.data.backup

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.gratitudelogger.domain.backup.BackupOutcome
import com.gratitudelogger.domain.backup.BackupProvider
import com.gratitudelogger.domain.backup.LastBackupInfo
import com.gratitudelogger.domain.backup.PendingBackupAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
private const val BACKUP_FILE_NAME = "gratitude-backup.zip"
private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()
private val ZIP_MEDIA_TYPE = "application/zip".toMediaType()

@Serializable
private data class DriveFile(val id: String, val name: String? = null)

@Serializable
private data class DriveFileList(val files: List<DriveFile> = emptyList())

@Serializable
private data class DriveFileMetadata(val name: String, val parents: List<String>? = null)

private sealed interface AuthorizationAttempt {
    data class Token(val accessToken: String) : AuthorizationAttempt
    data class NeedsResolution(val intentSender: IntentSender) : AuthorizationAttempt
    data class Failed(val message: String) : AuthorizationAttempt
}

/**
 * No access token is cached between calls - each backup/restore re-authorizes via
 * AuthorizationClient, which succeeds silently once the drive.appdata scope has already
 * been granted once, so there's nothing sensitive to store between calls.
 */
@Singleton
class GoogleDriveBackupProvider @Inject constructor(
    private val backupArchiver: BackupArchiver,
    private val backupPreferences: BackupPreferences
) : BackupProvider {

    private val httpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override val lastBackupInfo: Flow<LastBackupInfo?> = backupPreferences.lastBackupInfo

    override suspend fun backupNow(activity: Activity): BackupOutcome =
        when (val attempt = authorize(activity)) {
            is AuthorizationAttempt.Token -> performBackup(attempt.accessToken)
            is AuthorizationAttempt.NeedsResolution -> BackupOutcome.NeedsResolution(attempt.intentSender)
            is AuthorizationAttempt.Failed -> BackupOutcome.Failed(attempt.message)
        }

    override suspend fun restoreLatest(activity: Activity): BackupOutcome =
        when (val attempt = authorize(activity)) {
            is AuthorizationAttempt.Token -> performRestore(attempt.accessToken)
            is AuthorizationAttempt.NeedsResolution -> BackupOutcome.NeedsResolution(attempt.intentSender)
            is AuthorizationAttempt.Failed -> BackupOutcome.Failed(attempt.message)
        }

    override suspend fun resumeAfterResolution(
        activity: Activity,
        action: PendingBackupAction,
        resultCode: Int,
        data: Intent?
    ): BackupOutcome {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return BackupOutcome.Failed("Authorization was cancelled")
        }
        return try {
            val result = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
            val token = result.accessToken
                ?: return BackupOutcome.Failed("Authorization did not return an access token")
            when (action) {
                PendingBackupAction.BACKUP -> performBackup(token)
                PendingBackupAction.RESTORE -> performRestore(token)
            }
        } catch (e: Exception) {
            BackupOutcome.Failed(e.message ?: "Authorization failed")
        }
    }

    private suspend fun authorize(activity: Activity): AuthorizationAttempt {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
                .build()
            val result = Identity.getAuthorizationClient(activity).authorize(request).await()
            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                    ?: return AuthorizationAttempt.Failed("Authorization requires resolution but no intent was provided")
                AuthorizationAttempt.NeedsResolution(pendingIntent.intentSender)
            } else {
                val token = result.accessToken
                    ?: return AuthorizationAttempt.Failed("Authorization did not return an access token")
                AuthorizationAttempt.Token(token)
            }
        } catch (e: Exception) {
            AuthorizationAttempt.Failed(e.message ?: "Authorization failed")
        }
    }

    private suspend fun performBackup(accessToken: String): BackupOutcome = withContext(Dispatchers.IO) {
        try {
            val zipFile = backupArchiver.createBackupZip()
            val existingFileId = findBackupFileId(accessToken)
            uploadBackup(accessToken, zipFile, existingFileId)
            zipFile.delete()
            backupPreferences.recordBackup("Google Drive", Instant.now())
            BackupOutcome.Success
        } catch (e: Exception) {
            BackupOutcome.Failed(e.message ?: "Backup failed")
        }
    }

    private suspend fun performRestore(accessToken: String): BackupOutcome = withContext(Dispatchers.IO) {
        try {
            val fileId = findBackupFileId(accessToken)
                ?: return@withContext BackupOutcome.Failed("No backup was found in Google Drive")
            val zipBytes = downloadBackup(accessToken, fileId)
            backupArchiver.restoreFromZip(zipBytes)
        } catch (e: Exception) {
            BackupOutcome.Failed(e.message ?: "Restore failed")
        }
    }

    private fun findBackupFileId(accessToken: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name='$BACKUP_FILE_NAME'")
            .addQueryParameter("fields", "files(id,name)")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Drive list failed: ${response.code}")
            val body = response.body.string()
            val list = json.decodeFromString<DriveFileList>(body)
            return list.files.firstOrNull()?.id
        }
    }

    private fun uploadBackup(accessToken: String, zipFile: File, existingFileId: String?) {
        val metadata = if (existingFileId == null) {
            DriveFileMetadata(name = BACKUP_FILE_NAME, parents = listOf("appDataFolder"))
        } else {
            DriveFileMetadata(name = BACKUP_FILE_NAME)
        }
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(json.encodeToString(metadata).toRequestBody(JSON_MEDIA_TYPE))
            .addPart(zipFile.asRequestBody(ZIP_MEDIA_TYPE))
            .build()

        val url = if (existingFileId == null) {
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        } else {
            "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=multipart"
        }
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
        val request = if (existingFileId == null) {
            requestBuilder.post(body).build()
        } else {
            requestBuilder.patch(body).build()
        }
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Drive upload failed: ${response.code}")
        }
    }

    private fun downloadBackup(accessToken: String, fileId: String): ByteArray {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Drive download failed: ${response.code}")
            return response.body.bytes()
        }
    }
}
