package com.gratitudelogger.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gratitudelogger.ui.auth.PinSetupScreen
import com.gratitudelogger.ui.auth.SecurityGateViewModel
import com.gratitudelogger.ui.auth.UnlockScreen
import com.gratitudelogger.ui.navigation.GratitudeNavHost

@Composable
fun AppRoot(viewModel: SecurityGateViewModel = hiltViewModel()) {
    val isPinSet by viewModel.isPinSet.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()

    when (isPinSet) {
        null -> Unit
        false -> PinSetupScreen(onComplete = viewModel::onAuthenticated)
        true -> if (isLocked) {
            UnlockScreen(onUnlocked = viewModel::onAuthenticated)
        } else {
            GratitudeNavHost()
        }
    }
}
