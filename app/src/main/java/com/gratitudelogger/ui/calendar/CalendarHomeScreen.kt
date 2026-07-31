package com.gratitudelogger.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gratitudelogger.ui.theme.EntryDot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHomeScreen(
    viewModel: CalendarHomeViewModel = hiltViewModel(),
    onDayClick: (LocalDate) -> Unit = {},
    onAddEntryForToday: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val visibleMonth by viewModel.visibleMonth.collectAsStateWithLifecycle()
    val entryDates by viewModel.entryDates.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gratitude Logger") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntryForToday) {
                Icon(Icons.Default.Add, contentDescription = "Add today's entry")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MonthHeader(
                visibleMonth = visibleMonth,
                onPreviousMonth = viewModel::goToPreviousMonth,
                onNextMonth = viewModel::goToNextMonth,
                canGoNext = visibleMonth.isBefore(YearMonth.now())
            )
            WeekdayHeader()
            MonthGrid(
                visibleMonth = visibleMonth,
                entryDates = entryDates,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
private fun MonthHeader(
    visibleMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoNext: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}",
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = onNextMonth, enabled = canGoNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

private val weekdayLabels = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
)

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdayLabels.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun daysGridFor(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List<LocalDate?>(leadingBlanks) { null } + days
    val trailingBlanks = (7 - cells.size % 7) % 7
    return cells + List(trailingBlanks) { null }
}

@Composable
private fun MonthGrid(
    visibleMonth: YearMonth,
    entryDates: Set<LocalDate>,
    onDayClick: (LocalDate) -> Unit
) {
    val cells = daysGridFor(visibleMonth)
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(cells) { date ->
            DayCell(
                date = date,
                hasEntry = date != null && entryDates.contains(date),
                isToday = date != null && date == LocalDate.now(),
                onClick = { date?.let(onDayClick) }
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    hasEntry: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = if (isToday) {
                        MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    }
                )
                if (hasEntry) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(EntryDot, CircleShape)
                    )
                }
            }
        }
    }
}
