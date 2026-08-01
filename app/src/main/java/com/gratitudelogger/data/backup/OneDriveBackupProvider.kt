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
import kotlinx.serialization.SerialName
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
import java.io.RandomAccessFile
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OneDriveBackupProvider"

// App registration supports both personal Microsoft accounts and work/school accounts
// ("Accounts in any organizational directory and personal Microsoft accounts"), so the
// /common/ authority is used - /consumers/ would reject work/school sign-ins and
// /organizations/ would reject personal ones. Verified against Microsoft's own OAuth2
// authorization code flow reference during implementation.
private const val ONEDRIVE_AUTHORIZE_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
private const val ONEDRIVE_TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
private const val GRAPH_SCOPE = "https://graph.microsoft.com/Files.ReadWrite.AppFolder offline_access"

private const val ONEDRIVE_APPROOT_CONTENT_URL =
    "https://graph.microsoft.com/v1.0/me/drive/special/approot:/gratitude-backup.zip:/content"
private const val ONEDRIVE_CREATE_SESSION_URL =
    "https://graph.microsoft.com/v1.0/me/drive/special/approot:/gratitude-backup.zip:/createUploadSession"
private const val REDIRECT_URI = "gratitudelogger://oauth2redirect"

// Graph's simple PUT-to-content upload caps at 4 MiB; anything larger needs the chunked
// upload-session protocol below (unlike Drive/Dropbox, which both allow much larger single
// requests - this app's zip can plausibly exceed 4 MiB once photos are included).
private const val SIMPLE_UPLOAD_MAX_BYTES = 4L * 1024 * 1024
// Graph requires chunk sizes to be a multiple of 320 KiB; 5 MiB keeps the request count
// reasonable for a typical multi-photo backup without an oversized single PUT.
private const val CHUNK_SIZE = 5 * 1024 * 1024

private val ZIP_MEDIA_TYPE = "application/zip".toMediaType()
private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()

@Serializable
private data class OneDriveTokenResponse(
    val access_token: String,
    val refresh_token: String? = null
)

@Serializable
private data class OneDriveUploadSessionItem(
    @SerialName("@microsoft.graph.conflictBehavior") val conflictBehavior: String = "replace"
)

@Serializable
private data class OneDriveUploadSessionRequest(val item: OneDriveUploadSessionItem = OneDriveUploadSessionItem())

@Serializable
private data class OneDriveUploadSessionResponse(val uploadUrl: String)

/**
 * Only the refresh token is persisted (BackupPreferences.oneDriveRefreshToken), same "cache
 * nothing but a refresh token, re-derive an access token every call" model as
 * DropboxBackupProvider. Unlike Dropbox's non-expiring refresh token, Microsoft's is a rolling
 * ~90-day window - a refresh failure here is an expected occasional outcome for an
 * infrequently-used backup feature, not just a rare revoked-token edge case, but it's handled
 * identically either way: fall back to NeedsBrowserAuth.
 */
@Singleton
class OneDriveBackupProvider @Inject constructor(
    private val backupArchiver: BackupArchiver,
    private val backupPreferences: BackupPreferences
) : BackupProvider {

    private val httpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    // Survives the Custom Tab hop in the normal case (this is a @Singleton); a fresh auth
    // attempt is required if the process was killed in between, same tolerance as Dropbox's.
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
            token.refresh_token?.let { backupPreferences.setOneDriveRefreshToken(it) }
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
        val refreshToken = backupPreferences.currentOneDriveRefreshToken()
            ?: return BackupOutcome.NeedsBrowserAuth(buildAuthIntent())
        val accessToken = refreshAccessToken(refreshToken)
            ?: return BackupOutcome.NeedsBrowserAuth(buildAuthIntent())
        return block(accessToken)
    }

    private fun buildAuthIntent(): Intent {
        val verifier = Pkce.generateCodeVerifier().also { pendingCodeVerifier = it }
        val challenge = Pkce.codeChallengeFor(verifier)
        val url = ONEDRIVE_AUTHORIZE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("client_id", BuildConfig.ONEDRIVE_CLIENT_ID)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("scope", GRAPH_SCOPE)
            .addQueryParameter("code_challenge", challenge)
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("redirect_uri", REDIRECT_URI)
            .build()
        Log.d(TAG, "buildAuthIntent: $url")
        return CustomTabsIntent.Builder().build().intent.apply {
            data = url.toString().toUri()
        }
    }

    private suspend fun exchangeCodeForToken(code: String, verifier: String): OneDriveTokenResponse =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder()
                .add("code", code)
                .add("grant_type", "authorization_code")
                .add("client_id", BuildConfig.ONEDRIVE_CLIENT_ID)
                .add("scope", GRAPH_SCOPE)
                .add("redirect_uri", REDIRECT_URI)
                .add("code_verifier", verifier)
                .build()
            val request = Request.Builder().url(ONEDRIVE_TOKEN_URL).post(body).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body.string()
                    Log.e(TAG, "OneDrive token exchange failed: ${response.code} $errorBody")
                    throw IllegalStateException("OneDrive token exchange failed (${response.code}): $errorBody")
                }
                json.decodeFromString<OneDriveTokenResponse>(response.body.string())
            }
        }

    private suspend fun refreshAccessToken(refreshToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", BuildConfig.ONEDRIVE_CLIENT_ID)
                .add("scope", GRAPH_SCOPE)
                .build()
            val request = Request.Builder().url(ONEDRIVE_TOKEN_URL).post(body).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "OneDrive token refresh failed: ${response.code} ${response.body.string()}")
                    return@withContext null
                }
                json.decodeFromString<OneDriveTokenResponse>(response.body.string()).access_token
            }
        } catch (e: Exception) {
            Log.w(TAG, "OneDrive token refresh threw", e)
            null
        }
    }

    private suspend fun performBackup(accessToken: String): BackupOutcome = withContext(Dispatchers.IO) {
        try {
            val zipFile = backupArchiver.createBackupZip()
            uploadBackup(accessToken, zipFile)
            zipFile.delete()
            backupPreferences.recordBackup("OneDrive", Instant.now())
            BackupOutcome.Success
        } catch (e: Exception) {
            BackupOutcome.Failed(e.message ?: "Backup failed")
        }
    }

    private suspend fun performRestore(accessToken: String): BackupOutcome = withContext(Dispatchers.IO) {
        try {
            val zipBytes = downloadBackup(accessToken)
                ?: return@withContext BackupOutcome.Failed("No backup was found in OneDrive")
            backupArchiver.restoreFromZip(zipBytes)
        } catch (e: Exception) {
            BackupOutcome.Failed(e.message ?: "Restore failed")
        }
    }

    private fun uploadBackup(accessToken: String, zipFile: File) {
        if (zipFile.length() <= SIMPLE_UPLOAD_MAX_BYTES) {
            uploadSimple(accessToken, zipFile)
        } else {
            uploadChunked(accessToken, zipFile)
        }
    }

    private fun uploadSimple(accessToken: String, zipFile: File) {
        val request = Request.Builder()
            .url(ONEDRIVE_APPROOT_CONTENT_URL)
            .header("Authorization", "Bearer $accessToken")
            .put(zipFile.asRequestBody(ZIP_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("OneDrive upload failed: ${response.code}")
        }
    }

    private fun uploadChunked(accessToken: String, zipFile: File) {
        val sessionRequest = Request.Builder()
            .url(ONEDRIVE_CREATE_SESSION_URL)
            .header("Authorization", "Bearer $accessToken")
            .post(json.encodeToString(OneDriveUploadSessionRequest()).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val uploadUrl = httpClient.newCall(sessionRequest).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("OneDrive upload session failed: ${response.code}")
            json.decodeFromString<OneDriveUploadSessionResponse>(response.body.string()).uploadUrl
        }

        val totalSize = zipFile.length()
        RandomAccessFile(zipFile, "r").use { file ->
            var start = 0L
            while (start < totalSize) {
                val end = minOf(start + CHUNK_SIZE, totalSize)
                val chunk = ByteArray((end - start).toInt())
                file.seek(start)
                file.readFully(chunk)
                // The upload session URL is pre-authenticated - no Authorization header needed
                // (and Graph's own samples omit it here).
                val chunkRequest = Request.Builder()
                    .url(uploadUrl)
                    .header("Content-Range", "bytes $start-${end - 1}/$totalSize")
                    .put(chunk.toRequestBody())
                    .build()
                httpClient.newCall(chunkRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("OneDrive chunk upload failed: ${response.code}")
                    }
                }
                start = end
            }
        }
    }

    /** Returns null if no backup exists yet (Graph reports this as an HTTP 404). */
    private fun downloadBackup(accessToken: String): ByteArray? {
        val request = Request.Builder()
            .url(ONEDRIVE_APPROOT_CONTENT_URL)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) throw IllegalStateException("OneDrive download failed: ${response.code}")
            return response.body.bytes()
        }
    }
}
