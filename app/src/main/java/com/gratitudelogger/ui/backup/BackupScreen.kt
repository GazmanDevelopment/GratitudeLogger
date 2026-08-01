package com.gratitudelogger.ui.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gratitudelogger.domain.backup.BackupProviderType
import com.gratitudelogger.ui.theme.LocalHeaderColors
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val activity = LocalContext.current as Activity
    val lastBackupInfo by viewModel.lastBackupInfo.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingResolution by viewModel.pendingResolution.collectAsStateWithLifecycle()
    val pendingBrowserAuth by viewModel.pendingBrowserAuth.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val resolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onResolutionResult(activity, result.resultCode, result.data)
    }

    LaunchedEffect(pendingResolution) {
        pendingResolution?.let { intentSender ->
            resolutionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    // Dropbox's OAuth flow launches a Custom Tab via plain startActivity - unlike
    // pendingResolution above, there's no synchronous ActivityResult to wait for; the
    // eventual authorization code arrives later via oauthRedirects (see MainActivity.onNewIntent).
    LaunchedEffect(pendingBrowserAuth) {
        pendingBrowserAuth?.let { authIntent -> activity.startActivity(authIntent) }
    }

    LaunchedEffect(Unit) {
        viewModel.oauthRedirects.collect { uri -> viewModel.onRedirectReceived(activity, uri) }
    }

    val headerColors = LocalHeaderColors.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerColors.container,
                    titleContentColor = headerColors.content,
                    navigationIconContentColor = headerColors.content,
                    actionIconContentColor = headerColors.content
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Backup provider",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            BackupProviderType.entries.forEach { provider ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setSelectedProvider(provider) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = provider == selectedProvider,
                        onClick = { viewModel.setSelectedProvider(provider) }
                    )
                    Text(provider.displayName)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            val info = lastBackupInfo
            if (info != null) {
                val formatter = remember {
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
                }
                Text(
                    text = "Last backed up to ${info.accountLabel}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatter.format(
                        java.time.ZonedDateTime.ofInstant(info.timestamp, java.time.ZoneId.systemDefault())
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("No backup yet", style = MaterialTheme.typography.titleMedium)
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.backupNow(activity) },
                    enabled = !uiState.isWorking
                ) {
                    Text("Back up now")
                }
                OutlinedButton(
                    onClick = { showRestoreConfirm = true },
                    enabled = !uiState.isWorking
                ) {
                    Text("Restore latest")
                }
                if (uiState.isWorking) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore latest backup?") },
            text = {
                Text(
                    "This replaces all entries and photos currently on this device with the last " +
                        "backup from ${selectedProvider.displayName}. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    viewModel.restoreLatest(activity)
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
