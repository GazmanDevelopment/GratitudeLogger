package com.gratitudelogger.ui.backup

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gratitudelogger.domain.backup.BackupOutcome
import com.gratitudelogger.domain.backup.BackupProvider
import com.gratitudelogger.domain.backup.LastBackupInfo
import com.gratitudelogger.domain.backup.PendingBackupAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val backupProvider: BackupProvider
) : ViewModel() {

    val lastBackupInfo: StateFlow<LastBackupInfo?> = backupProvider.lastBackupInfo
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _pendingResolution = MutableStateFlow<IntentSender?>(null)
    val pendingResolution: StateFlow<IntentSender?> = _pendingResolution.asStateFlow()

    private var pendingAction: PendingBackupAction? = null

    fun backupNow(activity: Activity) {
        pendingAction = PendingBackupAction.BACKUP
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            handleOutcome(backupProvider.backupNow(activity))
        }
    }

    fun restoreLatest(activity: Activity) {
        pendingAction = PendingBackupAction.RESTORE
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            handleOutcome(backupProvider.restoreLatest(activity))
        }
    }

    fun onResolutionResult(activity: Activity, resultCode: Int, data: Intent?) {
        _pendingResolution.value = null
        val action = pendingAction ?: return
        viewModelScope.launch {
            handleOutcome(backupProvider.resumeAfterResolution(activity, action, resultCode, data))
        }
    }

    private fun handleOutcome(outcome: BackupOutcome) {
        when (outcome) {
            BackupOutcome.Success -> _uiState.update { it.copy(isWorking = false, error = null) }
            is BackupOutcome.NeedsResolution -> _pendingResolution.value = outcome.intentSender
            is BackupOutcome.Failed -> _uiState.update { it.copy(isWorking = false, error = outcome.message) }
        }
    }
}
