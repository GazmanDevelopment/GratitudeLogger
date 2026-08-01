package com.gratitudelogger.data.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.gratitudelogger.BuildConfig
import com.gratitudelogger.domain.backup.BackupOutcome
import com.gratitudelogger.domain.backup.BackupProvider
import com.gratitudelogger.domain.backup.LastBackupInfo
import com.gratitudelogger.domain.backup.PendingBackupAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DropboxBackupProvider"
private const val DROPBOX_AUTHORIZE_URL = "https://www.dropbox.com/oauth2/authorize"
private const val DROPBOX_TOKEN_URL = "https://api.dropbox.com/oauth2/token"
private const val DROPBOX_UPLOAD_URL = "https://content.dropboxapi.com/2/files/upload"
private const val DROPBOX_DOWNLOAD_URL = "https://content.dropboxapi.com/2/files/download"
private const val BACKUP_FILE_PATH = "/gratitude-backup.zip"
private const val REDIRECT_URI = "gratitudelogger://oauth2redirect"
private val OCTET_STREAM_MEDIA_TYPE = "application/octet-stream".toMediaType()

@Serializable
private data class DropboxTokenResponse(
    val access_token: String,
    val refresh_token: String? = null
)

@Serializable
private data class DropboxUploadArg(val path: String, val mode: String = "overwrite", val mute: Boolean = true)

@Serializable
private data class DropboxDownloadArg(val path: String)

/**
 * Only the refresh token is persisted (BackupPreferences.dropboxRefreshToken) - every call
 * exchanges it for a fresh access token via one cheap request rather than caching an access
 * token + its expiry, mirroring GoogleDriveBackupProvider's own "cache nothing but re-derive
 * cheaply" approach as closely as Dropbox's OAuth model allows.
 */
@Singleton
class DropboxBackupProvider @Inject constructor(
    private val backupArchiver: BackupArchiver,
    private val backupPreferences: BackupPreferences
) : BackupProvider {

    private val httpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    // Survives the Custom Tab hop in the normal case (this is a @Singleton); a fresh auth
    // attempt is required if the process was killed in between, same as this whole flow's
    // documented tolerance for that rare edge case.
    private var pendingCodeVerifier: String? = null

    override val lastBackupInfo: Flow<LastBackupInfo?> = backupPreferences.lastBackupInfo

    override suspend fun backupNow(activity: Activity): BackupOutcome = withAccessToken { performBackup(it) }

    override suspend fun restoreLatest(activity: Activity): BackupOutcome = withAccessToken { performRestore(it) }

    override suspend fun resumeAfterRedirect(
        activity: Activity,
        action: PendingBackupAction,
        redirectUri: Uri
    ): BackupOutcome {
        Log.d(TAG, "resumeAfterRedirect: $redirectUri")
        val code = redirectUri.getQueryParameter("code")
        if (code == null) {
            Log.w(TAG, "No 'code' query param in redirect - error=${redirectUri.getQueryParameter("error")}, " +
                "error_description=${redirectUri.getQueryParameter("error_description")}")
            return BackupOutcome.Failed("Authorization was cancelled")
        }
        val verifier = pendingCodeVerifier
            ?: return BackupOutcome.Failed("Authorization session expired - try again").also {
                Log.w(TAG, "resumeAfterRedirect called but pendingCodeVerifier was null")
            }
        return try {
            val token = exchangeCodeForToken(code, verifier)
            token.refresh_token?.let { backupPreferences.setDropboxRefreshToken(it) }
            when (action) {
                PendingBackupAction.BACKUP -> performBackup(token.access_token)
                PendingBackupAction.RESTORE -> performRestore(token.access_token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange/backup failed", e)
            BackupOutcome.Failed(e.message ?: "Authorization failed")
        } finally {
            pendingCodeVerifier = null
        }
    }

    private suspend fun withAccessToken(block: suspend (String) -> BackupOutcome): BackupOutcome {
        val refreshToken = backupPreferences.currentDropboxRefreshToken()
            ?: return BackupOutcome.NeedsBrowserAuth(buildAuthIntent())
        val accessToken = refreshAccessToken(refreshToken)
            ?: return BackupOutcome.NeedsBrowserAuth(buildAuthIntent())
        return block(accessToken)
    }

    private fun buildAuthIntent(): Intent {
        val verifier = Pkce.generateCodeVerifier().also { pendingCodeVerifier = it }
        val challenge = Pkce.codeChallengeFor(verifier)
        val url = DROPBOX_AUTHORIZE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("client_id", BuildConfig.DROPBOX_APP_KEY)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("code_challenge", challenge)
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("redirect_uri", REDIRECT_URI)
            .addQueryParameter("token_access_type", "offline")
            .build()
        Log.d(TAG, "buildAuthIntent: $url")
        return CustomTabsIntent.Builder().build().intent.apply {
            data = url.toString().toUri()
        }
    }

    private suspend fun exchangeCodeForToken(code: String, verifier: String): DropboxTokenResponse =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder()
                .add("code", code)
                .add("grant_type", "authorization_code")
                .add("client_id", BuildConfig.DROPBOX_APP_KEY)
                .add("redirect_uri", REDIRECT_URI)
                .add("code_verifier", verifier)
                .build()
            val request = Request.Builder().url(DROPBOX_TOKEN_URL).post(body).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body.string()
                    Log.e(TAG, "Dropbox token exchange failed: ${response.code} $errorBody")
                    throw IllegalStateException("Dropbox token exchange failed (${response.code}): $errorBody")
                }
                json.decodeFromString<DropboxTokenResponse>(response.body.string())
            }
        }

    private suspend fun refreshAccessToken(refreshToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", BuildConfig.DROPBOX_APP_KEY)
                .build()
            val request = Request.Builder().url(DROPBOX_TOKEN_URL).post(body).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Dropbox token refresh failed: ${response.code} ${response.body.string()}")
                    return@withContext null
                }
                json.decodeFromString<DropboxTokenResponse>(response.body.string()).access_token
            }
        } catch (e: Exception) {
            Log.w(TAG, "Dropbox token refresh threw", e)
            null
        }
    }

    private suspend fun performBackup(accessToken: String): BackupOutcome = withContext(Dispatchers.IO) {
        try {
            val zipFile = backupArchiver.createBackupZip()
            uploadBackup(accessToken, zipFile)
            zipFile.delete()
            backupPreferences.recordBackup("Dropbox", Instant.now())
            BackupOutcome.Success
        } catch (e: Exception) {
            BackupOutcome.Failed(e.message ?: "Backup failed")
        }
    }

    private suspend fun performRestore(accessToken: String): BackupOutcome = withContext(Dispatchers.IO) {
        try {
            val zipBytes = downloadBackup(accessToken)
                ?: return@withContext BackupOutcome.Failed("No backup was found in Dropbox")
            backupArchiver.restoreFromZip(zipBytes)
        } catch (e: Exception) {
            BackupOutcome.Failed(e.message ?: "Restore failed")
        }
    }

    private fun uploadBackup(accessToken: String, zipFile: File) {
        val apiArg = json.encodeToString(DropboxUploadArg(path = BACKUP_FILE_PATH))
        val request = Request.Builder()
            .url(DROPBOX_UPLOAD_URL)
            .header("Authorization", "Bearer $accessToken")
            .header("Dropbox-API-Arg", apiArg)
            .post(zipFile.asRequestBody(OCTET_STREAM_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Dropbox upload failed: ${response.code}")
        }
    }

    /** Returns null if no backup exists yet (Dropbox reports this as an HTTP 409 path error). */
    private fun downloadBackup(accessToken: String): ByteArray? {
        val apiArg = json.encodeToString(DropboxDownloadArg(path = BACKUP_FILE_PATH))
        val request = Request.Builder()
            .url(DROPBOX_DOWNLOAD_URL)
            .header("Authorization", "Bearer $accessToken")
            .header("Dropbox-API-Arg", apiArg)
            .post(ByteArray(0).toRequestBody())
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == 409) return null
            if (!response.isSuccessful) throw IllegalStateException("Dropbox download failed: ${response.code}")
            return response.body.bytes()
        }
    }
}
