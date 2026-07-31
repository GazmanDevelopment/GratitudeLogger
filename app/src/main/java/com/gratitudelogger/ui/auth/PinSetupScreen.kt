package com.gratitudelogger.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PinSetupScreen(
    viewModel: PinSetupViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val enteredPin by viewModel.enteredPin.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (phase) {
                PinSetupPhase.ENTER, PinSetupPhase.CONFIRM -> {
                    Text(
                        text = if (phase == PinSetupPhase.ENTER) "Create a PIN" else "Confirm your PIN",
                        style = MaterialTheme.typography.headlineSmall
                    )
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
                        onDigit = { digit -> viewModel.onDigit(digit, onComplete) },
                        onBackspace = viewModel::onBackspace
                    )
                }
                PinSetupPhase.BIOMETRIC_OPT_IN -> {
                    Text(
                        text = "Use biometric unlock?",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "You can use your fingerprint or face instead of your PIN to unlock the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(onClick = { viewModel.onBiometricOptIn(false, onComplete) }) {
                            Text("Not now")
                        }
                        Button(onClick = { viewModel.onBiometricOptIn(true, onComplete) }) {
                            Text("Enable")
                        }
                    }
                }
            }
        }
    }
}
