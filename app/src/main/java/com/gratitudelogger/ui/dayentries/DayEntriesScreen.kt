package com.gratitudelogger.ui.dayentries

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gratitudelogger.data.JournalEntry
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEntriesScreen(
    viewModel: DayEntriesViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onAddEntry: () -> Unit,
    onEditEntry: (Long) -> Unit
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val isToday = viewModel.date == LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.date.toString()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isToday) {
                FloatingActionButton(onClick = onAddEntry) {
                    Icon(Icons.Default.Add, contentDescription = "Add entry")
                }
            }
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                Text(
                    text = "No entries yet",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                items(entries, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        onClick = { onEditEntry(entry.id) },
                        onDelete = { viewModel.deleteEntry(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: JournalEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit entry")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete entry")
            }
        }
    }
}
