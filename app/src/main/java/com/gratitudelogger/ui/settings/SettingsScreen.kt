package com.gratitudelogger.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onChangePin: () -> Unit
) {
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderTime by viewModel.reminderTime.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Proceed either way - the reminder is scheduled regardless; without the
        // permission the notification just silently won't post (see ReminderNotifier).
        viewModel.setReminderEnabled(true)
    }

    fun requestExactAlarmIfNeeded() {
        if (!viewModel.canScheduleExactAlarms()) {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }

    fun onReminderToggle(checked: Boolean) {
        if (checked) {
            requestExactAlarmIfNeeded()
            val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setReminderEnabled(true)
            }
        } else {
            viewModel.setReminderEnabled(false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily reminder", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "%02d:%02d".format(reminderTime.hour, reminderTime.minute),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(checked = reminderEnabled, onCheckedChange = ::onReminderToggle)
            }
            if (reminderEnabled) {
                TextButton(onClick = { showTimePicker = true }) {
                    Text("Change time")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "Security",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Biometric unlock", modifier = Modifier.weight(1f))
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = viewModel::setBiometricEnabled,
                    enabled = viewModel.isBiometricAvailable
                )
            }
            viewModel.biometricUnavailableReason?.let { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            TextButton(onClick = onChangePin) {
                Text("Change PIN")
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderTime.hour,
            initialMinute = reminderTime.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReminderTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}
