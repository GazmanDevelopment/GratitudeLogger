package com.gratitudelogger.ui.dayentries

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gratitudelogger.data.JournalEntry
import com.gratitudelogger.domain.JournalRepository
import com.gratitudelogger.ui.navigation.DayEntriesRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DayEntriesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository
) : ViewModel() {

    val date: LocalDate = LocalDate.ofEpochDay(savedStateHandle.toRoute<DayEntriesRoute>().epochDay)

    val entries: StateFlow<List<JournalEntry>> = repository.entriesForDate(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }
}
