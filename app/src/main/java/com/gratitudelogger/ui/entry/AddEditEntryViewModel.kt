package com.gratitudelogger.ui.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gratitudelogger.data.JournalEntry
import com.gratitudelogger.domain.JournalRepository
import com.gratitudelogger.ui.navigation.AddEditEntryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository
) : ViewModel() {

    private val entryId: Long? = savedStateHandle.toRoute<AddEditEntryRoute>().entryId
    val isEditing: Boolean = entryId != null

    private var existingEntry: JournalEntry? = null

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    init {
        if (entryId != null) {
            viewModelScope.launch {
                val entry = repository.entryById(entryId).first()
                existingEntry = entry
                _text.value = entry?.text.orEmpty()
            }
        }
    }

    fun onTextChange(newText: String) {
        _text.value = newText
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = existingEntry
            if (current != null) {
                repository.updateEntry(current.copy(text = _text.value))
            } else {
                repository.addEntry(_text.value)
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val current = existingEntry ?: return
        viewModelScope.launch {
            repository.deleteEntry(current)
            onDone()
        }
    }
}
