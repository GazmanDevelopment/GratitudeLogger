package com.gratitudelogger.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UnlockScreen(
    viewModel: UnlockViewModel = hiltViewModel(),
    onUnlocked: () -> Unit
) {
    val enteredPin by viewModel.enteredPin.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? FragmentActivity

    val canUseBiometric = remember(biometricEnabled, activity) {
        biometricEnabled && activity != null &&
            BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric && activity != null) {
            showBiometricPrompt(activity, onUnlocked)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Enter your PIN", style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                PinDotsIndicator(length = enteredPin.length)
            }
            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            PinPad(
                onDigit = { digit -> viewModel.onDigit(digit, onUnlocked) },
                onBackspace = viewModel::onBackspace
            )
            if (canUseBiometric && activity != null) {
                TextButton(onClick = { showBiometricPrompt(activity, onUnlocked) }) {
                    Text("Use biometrics instead")
                }
            }
        }
    }
}

private fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        }
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Gratitude Logger")
        .setNegativeButtonText("Use PIN")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()
    prompt.authenticate(promptInfo)
}
