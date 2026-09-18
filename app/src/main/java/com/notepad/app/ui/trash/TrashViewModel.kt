package com.notepad.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepad.app.domain.model.Note
import com.notepad.app.domain.repository.NoteRepository
import com.notepad.app.domain.usecase.DeleteNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val trashNotes: List<Note> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        repository.getTrashNotes().onEach { notes ->
            _uiState.update { it.copy(trashNotes = notes, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun restoreNote(noteId: Long) {
        viewModelScope.launch {
            deleteNoteUseCase.restore(noteId)
        }
    }

    fun hardDeleteNote(noteId: Long) {
        viewModelScope.launch {
            deleteNoteUseCase.hardDelete(noteId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            deleteNoteUseCase.clearTrash()
        }
    }
}
