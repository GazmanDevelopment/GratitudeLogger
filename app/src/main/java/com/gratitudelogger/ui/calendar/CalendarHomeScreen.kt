package com.gratitudelogger.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gratitudelogger.data.JournalEntry
import com.gratitudelogger.ui.theme.LocalHeaderColors
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val PaleRedBackground = Color(0xFFFDE1E4)
private val DeleteRevealColor = Color(0xFFE53935)
private val EditRevealColor = Color(0xFF43A047)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHomeScreen(
    viewModel: CalendarHomeViewModel = hiltViewModel(),
    onAddEntryForToday: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onEditEntry: (Long) -> Unit = {}
) {
    val today = viewModel.today
    val earliestDate by viewModel.earliestDate.collectAsStateWithLifecycle()
    val entriesByDate by viewModel.entriesByDate.collectAsStateWithLifecycle()

    val dayCount = remember(earliestDate, today) {
        ChronoUnit.DAYS.between(earliestDate, today).toInt() + 1
    }
    fun dateForIndex(index: Int): LocalDate = today.minusDays(index.toLong())

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    fun scrollToDate(date: LocalDate) {
        // The feed has no rows before earliestDate - nothing to scroll to there.
        if (date.isBefore(earliestDate) || date.isAfter(today)) return
        val index = ChronoUnit.DAYS.between(date, today).toInt().coerceIn(0, dayCount - 1)
        scope.launch { listState.animateScrollToItem(index) }
    }

    // The displayed month is explicit state, not purely derived from scroll: months before
    // the earliest entry have no corresponding feed rows to scroll to at all, so browsing
    // there has to set the displayed month directly. The feed-scroll effect below still keeps
    // it in sync whenever the user drags the feed themselves (or a programmatic scroll lands
    // it on a different month than what was just set) - both paths write the same state, so
    // they can't fight, only agree.
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index -> visibleMonth = YearMonth.from(dateForIndex(index)) }
    }

    fun changeMonth(target: YearMonth) {
        val movingBackward = target.isBefore(visibleMonth)
        visibleMonth = target
        if (!target.isBefore(YearMonth.from(earliestDate))) {
            val landingDate = if (movingBackward) target.atEndOfMonth() else target.atDay(1)
            scrollToDate(maxOf(minOf(landingDate, today), earliestDate))
        }
        // else: no feed rows exist for this month - just show it without moving the feed.
    }

    val entryDatesInVisibleMonth = remember(visibleMonth, entriesByDate) {
        entriesByDate.keys.filter { YearMonth.from(it) == visibleMonth }.toSet()
    }

    var pendingDelete by remember { mutableStateOf<JournalEntry?>(null) }

    val headerColors = LocalHeaderColors.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gratitude Logger") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerColors.container,
                    titleContentColor = headerColors.content,
                    navigationIconContentColor = headerColors.content,
                    actionIconContentColor = headerColors.content
                )
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
                onPreviousMonth = { changeMonth(visibleMonth.minusMonths(1)) },
                onNextMonth = {
                    if (visibleMonth.isBefore(YearMonth.now())) changeMonth(visibleMonth.plusMonths(1))
                },
                canGoNext = visibleMonth.isBefore(YearMonth.now())
            )
            WeekdayHeader()
            MonthGrid(
                visibleMonth = visibleMonth,
                entryDates = entryDatesInVisibleMonth,
                today = today,
                onDayClick = { date -> scrollToDate(date) }
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(
                    count = dayCount,
                    key = { i -> dateForIndex(i).toEpochDay() }
                ) { i ->
                    val date = dateForIndex(i)
                    DaySection(
                        date = date,
                        entries = entriesByDate[date].orEmpty(),
                        resolvePhotoFile = viewModel::resolvePhotoFile,
                        onEditEntry = onEditEntry,
                        onRequestDelete = { entry -> pendingDelete = entry }
                    )
                }
            }
        }
    }

    val entryToDelete = pendingDelete
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this entry?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(entryToDelete)
                    pendingDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
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

// Deliberately NOT a LazyVerticalGrid: a Lazy layout given a bounded max-height constraint
// by the parent Column always fills that entire constraint as its viewport, which would
// starve the LazyColumn feed below it of any space. A plain Column/Row grid wraps to its
// actual (fixed, ~6-row) content height instead, letting the feed's Modifier.weight(1f)
// claim the remaining space correctly.
@Composable
private fun MonthGrid(
    visibleMonth: YearMonth,
    entryDates: Set<LocalDate>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    val rows = remember(visibleMonth) { daysGridFor(visibleMonth).chunked(7) }
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        hasEntry = date != null && entryDates.contains(date),
                        isToday = date != null && date == today,
                        onClick = { date?.let(onDayClick) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    hasEntry: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    )
                }
            }
        }
    }
}

private val dayHeaderFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())

@Composable
private fun DaySection(
    date: LocalDate,
    entries: List<JournalEntry>,
    resolvePhotoFile: (String) -> File,
    onEditEntry: (Long) -> Unit,
    onRequestDelete: (JournalEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            text = dayHeaderFormatter.format(date),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaleRedBackground)
                    .padding(16.dp)
            ) {
                Text("No entries on this day", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            entries.forEach { entry ->
                key(entry.id) {
                    SwipeableEntryRow(
                        entry = entry,
                        photoFile = entry.photoPath?.let(resolvePhotoFile),
                        onEditEntry = onEditEntry,
                        onRequestDelete = onRequestDelete
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEntryRow(
    entry: JournalEntry,
    photoFile: File?,
    onEditEntry: (Long) -> Unit,
    onRequestDelete: (JournalEntry) -> Unit
) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()

    SwipeToDismissBox(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.EndToStart -> onRequestDelete(entry)
                SwipeToDismissBoxValue.StartToEnd -> onEditEntry(entry.id)
                SwipeToDismissBoxValue.Settled -> Unit
            }
            // Always spring back - the row's actual disappearance (on confirmed delete)
            // comes from Room's Flow dropping it, not from leaving the box dismissed.
            scope.launch { state.reset() }
        },
        backgroundContent = {
            val (color, icon, alignment) = when (state.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Triple(DeleteRevealColor, Icons.Default.Delete, Alignment.CenterEnd)
                SwipeToDismissBoxValue.StartToEnd -> Triple(EditRevealColor, Icons.Default.Edit, Alignment.CenterStart)
                SwipeToDismissBoxValue.Settled -> Triple(Color.Transparent, null, Alignment.Center)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                icon?.let { Icon(it, contentDescription = null, tint = Color.White) }
            }
        }
    ) {
        EntryCard(entry = entry, photoFile = photoFile)
    }
}

@Composable
private fun EntryCard(entry: JournalEntry, photoFile: File?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (photoFile != null) {
                AsyncImage(
                    model = photoFile,
                    contentDescription = "Entry photo",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
