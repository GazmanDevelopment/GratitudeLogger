package com.gratitudelogger.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gratitudelogger.security.SecurityPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val securityPreferences: SecurityPreferences
) : ViewModel() {

    val biometricEnabled: StateFlow<Boolean> = securityPreferences.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun onDigit(digit: Char, onUnlocked: () -> Unit) {
        if (_enteredPin.value.length >= PIN_LENGTH) return
        val next = _enteredPin.value + digit
        _enteredPin.value = next
        if (next.length == PIN_LENGTH) {
            viewModelScope.launch {
                if (securityPreferences.verifyPin(next)) {
                    // This ViewModel is Activity-scoped and survives stop/restart (only
                    // onCleared on a true Activity finish), so the entered PIN must be reset
                    // here - otherwise the next lock cycle reuses this same instance and shows
                    // the pad already full of the previously-successful digits.
                    _enteredPin.value = ""
                    _error.value = null
                    onUnlocked()
                } else {
                    _error.value = "Incorrect PIN"
                    _enteredPin.value = ""
                }
            }
        }
    }

    fun onBackspace() {
        _enteredPin.value = _enteredPin.value.dropLast(1)
        _error.value = null
    }
}
