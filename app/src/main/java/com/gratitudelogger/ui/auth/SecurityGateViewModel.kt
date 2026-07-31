package com.gratitudelogger.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gratitudelogger.security.AppLockManager
import com.gratitudelogger.security.SecurityPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SecurityGateViewModel @Inject constructor(
    securityPreferences: SecurityPreferences,
    private val appLockManager: AppLockManager
) : ViewModel() {

    val isPinSet: StateFlow<Boolean?> = securityPreferences.isPinSet
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLocked: StateFlow<Boolean> = appLockManager.isLocked

    fun onAuthenticated() {
        appLockManager.unlock()
    }
}
