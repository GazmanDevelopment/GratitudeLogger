package com.gratitudelogger.ui.entry

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gratitudelogger.data.JournalEntry
import com.gratitudelogger.domain.JournalRepository
import com.gratitudelogger.domain.PhotoStorage
import com.gratitudelogger.ui.navigation.AddEditEntryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddEditEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
    private val photoStorage: PhotoStorage
) : ViewModel() {

    private val entryId: Long? = savedStateHandle.toRoute<AddEditEntryRoute>().entryId
    val isEditing: Boolean = entryId != null

    private var existingEntry: JournalEntry? = null
    private var pendingCapturePath: String? = null

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _photoPath = MutableStateFlow<String?>(null)
    val photoPath: StateFlow<String?> = _photoPath.asStateFlow()

    init {
        if (entryId != null) {
            viewModelScope.launch {
                val entry = repository.entryById(entryId).first()
                existingEntry = entry
                _text.value = entry?.text.orEmpty()
                _photoPath.value = entry?.photoPath
            }
        }
    }

    fun onTextChange(newText: String) {
        _text.value = newText
    }

    fun prepareCaptureTarget(): Uri {
        val target = photoStorage.createCaptureTarget()
        pendingCapturePath = target.relativePath
        return target.uri
    }

    fun onCaptureResult(success: Boolean) {
        val path = pendingCapturePath
        pendingCapturePath = null
        if (success && path != null) {
            viewModelScope.launch {
                photoStorage.finalizeCapturedPhoto(path)
                _photoPath.value = path
            }
        } else if (path != null) {
            viewModelScope.launch { photoStorage.deletePhoto(path) }
        }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val path = photoStorage.savePickedPhoto(uri)
            _photoPath.value = path
        }
    }

    fun removePhoto() {
        _photoPath.value = null
    }

    fun resolvePhotoFile(relativePath: String): File = photoStorage.resolveFile(relativePath)

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = existingEntry
            val newPhotoPath = _photoPath.value
            if (current != null) {
                if (current.photoPath != null && current.photoPath != newPhotoPath) {
                    photoStorage.deletePhoto(current.photoPath)
                }
                repository.updateEntry(current.copy(text = _text.value, photoPath = newPhotoPath))
            } else {
                repository.addEntry(_text.value, newPhotoPath)
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
