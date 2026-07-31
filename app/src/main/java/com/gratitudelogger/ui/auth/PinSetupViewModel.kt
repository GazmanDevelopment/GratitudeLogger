package com.gratitudelogger.ui.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gratitudelogger.security.SecurityPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PinSetupPhase { ENTER, CONFIRM, BIOMETRIC_OPT_IN }

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    private val securityPreferences: SecurityPreferences,
    @ApplicationContext context: Context
) : ViewModel() {

    val isBiometricAvailable: Boolean =
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private val _phase = MutableStateFlow(PinSetupPhase.ENTER)
    val phase: StateFlow<PinSetupPhase> = _phase.asStateFlow()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var firstPin: String = ""

    fun onDigit(digit: Char, onComplete: () -> Unit) {
        if (_enteredPin.value.length >= PIN_LENGTH) return
        val next = _enteredPin.value + digit
        _enteredPin.value = next
        if (next.length == PIN_LENGTH) {
            when (_phase.value) {
                PinSetupPhase.ENTER -> {
                    firstPin = next
                    _enteredPin.value = ""
                    _phase.value = PinSetupPhase.CONFIRM
                }
                PinSetupPhase.CONFIRM -> confirmPin(next, onComplete)
                PinSetupPhase.BIOMETRIC_OPT_IN -> Unit
            }
        }
    }

    private fun confirmPin(confirmed: String, onComplete: () -> Unit) {
        if (confirmed != firstPin) {
            _error.value = "PINs didn't match, try again"
            _enteredPin.value = ""
            _phase.value = PinSetupPhase.ENTER
            firstPin = ""
            return
        }
        _error.value = null
        viewModelScope.launch {
            securityPreferences.setPin(confirmed)
            if (isBiometricAvailable) {
                _phase.value = PinSetupPhase.BIOMETRIC_OPT_IN
            } else {
                onComplete()
            }
        }
    }

    fun onBackspace() {
        _enteredPin.value = _enteredPin.value.dropLast(1)
    }

    fun onBiometricOptIn(enabled: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            securityPreferences.setBiometricEnabled(enabled)
            onComplete()
        }
    }
}
