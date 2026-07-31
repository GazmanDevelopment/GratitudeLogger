package com.gratitudelogger.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.gratitudelogger.ui.auth.PinSetupScreen
import com.gratitudelogger.ui.auth.SecurityGateViewModel
import com.gratitudelogger.ui.auth.UnlockScreen
import com.gratitudelogger.ui.navigation.GratitudeNavHost

@Composable
fun AppRoot(viewModel: SecurityGateViewModel = hiltViewModel()) {
    val isPinSet by viewModel.isPinSet.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()

    // Created unconditionally at this top level (rather than inside GratitudeNavHost's
    // default parameter) so the back stack survives lock/unlock cycles - otherwise every
    // relock swaps this composable out for UnlockScreen and back, which would otherwise
    // forget the NavController and drop the user back at the start destination.
    val navController = rememberNavController()

    when (isPinSet) {
        null -> Unit
        false -> PinSetupScreen(onComplete = viewModel::onAuthenticated)
        true -> if (isLocked) {
            UnlockScreen(onUnlocked = viewModel::onAuthenticated)
        } else {
            GratitudeNavHost(navController = navController)
        }
    }
}
