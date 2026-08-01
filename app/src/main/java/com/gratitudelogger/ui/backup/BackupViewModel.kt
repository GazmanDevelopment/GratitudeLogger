package com.gratitudelogger.ui.backup

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gratitudelogger.data.backup.BackupPreferences
import com.gratitudelogger.data.backup.DropboxBackupProvider
import com.gratitudelogger.data.backup.GoogleDriveBackupProvider
import com.gratitudelogger.data.backup.OAuthRedirectRelay
import com.gratitudelogger.domain.backup.BackupOutcome
import com.gratitudelogger.domain.backup.BackupProvider
import com.gratitudelogger.domain.backup.BackupProviderType
import com.gratitudelogger.domain.backup.LastBackupInfo
import com.gratitudelogger.domain.backup.PendingBackupAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isWorking: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val googleDriveBackupProvider: GoogleDriveBackupProvider,
    private val dropboxBackupProvider: DropboxBackupProvider,
    private val backupPreferences: BackupPreferences,
    oauthRedirectRelay: OAuthRedirectRelay
) : ViewModel() {

    val lastBackupInfo: StateFlow<LastBackupInfo?> = backupPreferences.lastBackupInfo
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val selectedProvider: StateFlow<BackupProviderType> = backupPreferences.selectedProvider
        .stateIn(viewModelScope, SharingStarted.Eagerly, BackupProviderType.GOOGLE_DRIVE)

    val oauthRedirects: SharedFlow<Uri> = oauthRedirectRelay.redirects

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _pendingResolution = MutableStateFlow<IntentSender?>(null)
    val pendingResolution: StateFlow<IntentSender?> = _pendingResolution.asStateFlow()

    private val _pendingBrowserAuth = MutableStateFlow<Intent?>(null)
    val pendingBrowserAuth: StateFlow<Intent?> = _pendingBrowserAuth.asStateFlow()

    private var pendingAction: PendingBackupAction? = null

    // Whichever provider actually issued the in-flight auth attempt - resumption must call
    // back into that same instance, not necessarily whatever's currently selected, in case the
    // selection changes while a resolution/redirect is still pending.
    private var activeProviderForPendingAction: BackupProvider? = null

    fun setSelectedProvider(provider: BackupProviderType) {
        viewModelScope.launch { backupPreferences.setSelectedProvider(provider) }
    }

    private fun providerFor(type: BackupProviderType): BackupProvider = when (type) {
        BackupProviderType.GOOGLE_DRIVE -> googleDriveBackupProvider
        BackupProviderType.DROPBOX -> dropboxBackupProvider
    }

    fun backupNow(activity: Activity) {
        pendingAction = PendingBackupAction.BACKUP
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            val provider = providerFor(backupPreferences.currentSelectedProvider())
            activeProviderForPendingAction = provider
            handleOutcome(provider.backupNow(activity))
        }
    }

    fun restoreLatest(activity: Activity) {
        pendingAction = PendingBackupAction.RESTORE
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            val provider = providerFor(backupPreferences.currentSelectedProvider())
            activeProviderForPendingAction = provider
            handleOutcome(provider.restoreLatest(activity))
        }
    }

    fun onResolutionResult(activity: Activity, resultCode: Int, data: Intent?) {
        _pendingResolution.value = null
        val action = pendingAction ?: return
        val provider = activeProviderForPendingAction ?: return
        viewModelScope.launch {
            handleOutcome(provider.resumeAfterResolution(activity, action, resultCode, data))
        }
    }

    fun onRedirectReceived(activity: Activity, uri: Uri) {
        _pendingBrowserAuth.value = null
        val action = pendingAction ?: return
        val provider = activeProviderForPendingAction ?: return
        viewModelScope.launch {
            handleOutcome(provider.resumeAfterRedirect(activity, action, uri))
        }
    }

    private fun handleOutcome(outcome: BackupOutcome) {
        when (outcome) {
            BackupOutcome.Success -> _uiState.update { it.copy(isWorking = false, error = null) }
            is BackupOutcome.NeedsResolution -> _pendingResolution.value = outcome.intentSender
            is BackupOutcome.NeedsBrowserAuth -> _pendingBrowserAuth.value = outcome.authIntent
            is BackupOutcome.Failed -> _uiState.update { it.copy(isWorking = false, error = outcome.message) }
        }
    }
}
