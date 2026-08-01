package com.gratitudelogger.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    val isAuthenticated = isPinSet == true && !isLocked
    LaunchedEffect(isAuthenticated) {
        // The daily reminder is enabled by default (ReminderPreferences), so request the
        // notification permission proactively here rather than only when the user happens
        // to open Settings - otherwise the reminder is silently scheduled but never shown
        // on API 33+. Only fires once actually inside the authenticated area, not over the
        // PIN setup/unlock screens; a no-op if already granted or permanently denied.
        if (isAuthenticated &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
