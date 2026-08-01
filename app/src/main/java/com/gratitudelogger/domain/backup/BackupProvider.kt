package com.gratitudelogger.domain.backup

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class LastBackupInfo(val accountLabel: String, val timestamp: Instant)

enum class BackupProviderType(val displayName: String) {
    GOOGLE_DRIVE("Google Drive"),
    DROPBOX("Dropbox"),
    ONEDRIVE("OneDrive")
}

sealed interface BackupOutcome {
    data object Success : BackupOutcome
    data class NeedsResolution(val intentSender: IntentSender) : BackupOutcome
    data class NeedsBrowserAuth(val authIntent: Intent) : BackupOutcome
    data class Failed(val message: String) : BackupOutcome
}

enum class PendingBackupAction { BACKUP, RESTORE }

/**
 * Provider-agnostic so a future OneDrive/Dropbox implementation can sit behind the same
 * interface. No access token is cached between calls - each backupNow/restoreLatest
 * re-authorizes (silently, once already granted) rather than the app storing an OAuth
 * secret anywhere.
 */
interface BackupProvider {
    val lastBackupInfo: Flow<LastBackupInfo?>

    suspend fun backupNow(activity: Activity): BackupOutcome

    suspend fun restoreLatest(activity: Activity): BackupOutcome

    /** Resumes a flow that needed [BackupOutcome.NeedsResolution]. Not every provider uses this. */
    suspend fun resumeAfterResolution(
        activity: Activity,
        action: PendingBackupAction,
        resultCode: Int,
        data: Intent?
    ): BackupOutcome = BackupOutcome.Failed("Not supported by this provider")

    /** Resumes a flow that needed [BackupOutcome.NeedsBrowserAuth]. Not every provider uses this. */
    suspend fun resumeAfterRedirect(
        activity: Activity,
        action: PendingBackupAction,
        redirectUri: Uri
    ): BackupOutcome = BackupOutcome.Failed("Not supported by this provider")
}
