package com.notepad.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepad.app.core.template.NoteTemplate
import com.notepad.app.core.theme.AppThemeMode
import com.notepad.app.core.theme.ThemePreferences
import com.notepad.app.domain.model.NoteCategory
import com.notepad.app.domain.model.SortOrder
import com.notepad.app.domain.repository.NoteRepository
import com.notepad.app.domain.usecase.DeleteNoteUseCase
import com.notepad.app.domain.usecase.GetNotesUseCase
import com.notepad.app.domain.usecase.PurgeOldTrashUseCase
import com.notepad.app.domain.usecase.SearchNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.FlowPreview

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val purgeOldTrashUseCase: PurgeOldTrashUseCase,
    private val repository: NoteRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState(isLoading = true))
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<NotesUiEffect>()
    val uiEffect: SharedFlow<NotesUiEffect> = _uiEffect.asSharedFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Auto purge soft-deleted notes older than 30 days
        viewModelScope.launch {
            purgeOldTrashUseCase()
        }

        // Observe theme preference
        themePreferences.themeMode.onEach { theme ->
            _uiState.update { it.copy(currentTheme = theme) }
        }.launchIn(viewModelScope)

        // Combine category, sort order, and debounced search query
        combine(
            _uiState.mapDistinct { it.selectedCategory },
            _uiState.mapDistinct { it.sortOrder },
            searchQueryFlow.debounce(250).distinctUntilChanged()
        ) { category, sortOrder, query ->
            Triple(category, sortOrder, query)
        }.flatMapLatest { (category, sortOrder, query) ->
            if (query.isBlank()) {
                getNotesUseCase(category, sortOrder)
            } else {
                searchNotesUseCase(query)
            }
        }.onEach { notes ->
            _uiState.update { it.copy(notes = notes, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: NotesUiEvent) {
        when (event) {
            is NotesUiEvent.OnSearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query, isSearchActive = event.query.isNotBlank()) }
                searchQueryFlow.value = event.query
            }
            is NotesUiEvent.OnCategorySelected -> {
                _uiState.update { it.copy(selectedCategory = event.category) }
            }
            is NotesUiEvent.OnSortOrderChanged -> {
                _uiState.update { it.copy(sortOrder = event.sortOrder) }
            }
            is NotesUiEvent.OnTogglePin -> {
                viewModelScope.launch {
                    repository.togglePin(event.noteId)
                }
            }
            is NotesUiEvent.OnDeleteNote -> {
                viewModelScope.launch {
                    deleteNoteUseCase.softDelete(event.noteId)
                    _uiEffect.emit(NotesUiEffect.ShowSnackbar("Note moved to Trash"))
                }
            }
            is NotesUiEvent.OnNoteClicked -> {
                viewModelScope.launch {
                    _uiEffect.emit(NotesUiEffect.NavigateToEditor(event.noteId))
                }
            }
            is NotesUiEvent.OnCreateNoteClicked -> {
                viewModelScope.launch {
                    _uiEffect.emit(NotesUiEffect.NavigateToEditor(0L))
                }
            }
            is NotesUiEvent.OnVaultClicked -> {
                viewModelScope.launch {
                    _uiEffect.emit(NotesUiEffect.NavigateToVault)
                }
            }
            is NotesUiEvent.OnTrashClicked -> {
                viewModelScope.launch {
                    _uiEffect.emit(NotesUiEffect.NavigateToTrash)
                }
            }
            is NotesUiEvent.OnShowThemePicker -> {
                _uiState.update { it.copy(isThemePickerVisible = true) }
            }
            is NotesUiEvent.OnDismissThemePicker -> {
                _uiState.update { it.copy(isThemePickerVisible = false) }
            }
            is NotesUiEvent.OnThemeSelected -> {
                viewModelScope.launch {
                    themePreferences.setTheme(event.theme)
                    _uiState.update { it.copy(currentTheme = event.theme, isThemePickerVisible = false) }
                }
            }
            is NotesUiEvent.OnShowTemplatePicker -> {
                _uiState.update { it.copy(isTemplatePickerVisible = true) }
            }
            is NotesUiEvent.OnDismissTemplatePicker -> {
                _uiState.update { it.copy(isTemplatePickerVisible = false) }
            }
            is NotesUiEvent.OnTemplateSelected -> {
                _uiState.update { it.copy(isTemplatePickerVisible = false) }
                viewModelScope.launch {
                    _uiEffect.emit(NotesUiEffect.NavigateToEditorWithTemplate(event.template))
                }
            }
        }
    }

    private fun <T, R> StateFlow<T>.mapDistinct(transform: (T) -> R): kotlinx.coroutines.flow.Flow<R> {
        return kotlinx.coroutines.flow.flow {
            var lastValue: Any? = Any()
            collect { value ->
                val transformed = transform(value)
                if (transformed != lastValue) {
                    lastValue = transformed
                    emit(transformed)
                }
            }
        }
    }
}

