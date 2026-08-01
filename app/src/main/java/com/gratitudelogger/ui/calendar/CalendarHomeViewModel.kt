package com.gratitudelogger.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gratitudelogger.data.JournalEntry
import com.gratitudelogger.domain.JournalRepository
import com.gratitudelogger.domain.PhotoStorage
import com.gratitudelogger.theme.EntryOrder
import com.gratitudelogger.theme.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarHomeViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val photoStorage: PhotoStorage,
    themePreferences: ThemePreferences
) : ViewModel() {

    // Fixed for the ViewModel's lifetime - matches this codebase's existing precedent of not
    // reactively handling midnight rollover (e.g. AddEditEntryViewModel's LocalDate.now() calls).
    val today: LocalDate = LocalDate.now()

    private val allEntries: StateFlow<List<JournalEntry>> = repository.allEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val earliestDate: StateFlow<LocalDate> = allEntries
        .map { list -> list.minOfOrNull { it.entryDate } ?: today }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), today)

    val entriesByDate: StateFlow<Map<LocalDate, List<JournalEntry>>> = allEntries
        .map { list -> list.groupBy { it.entryDate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val entryOrder: StateFlow<EntryOrder> = themePreferences.selectedEntryOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryOrder.NEWEST_FIRST)

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    fun resolvePhotoFile(relativePath: String): File = photoStorage.resolveFile(relativePath)
}
