package com.notepad.app.ui.notes

import com.notepad.app.core.template.NoteTemplate
import com.notepad.app.core.theme.AppThemeMode
import com.notepad.app.domain.model.Note
import com.notepad.app.domain.model.NoteCategory
import com.notepad.app.domain.model.SortOrder

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: NoteCategory = NoteCategory.ALL,
    val sortOrder: SortOrder = SortOrder.DATE_MODIFIED_DESC,
    val isLoading: Boolean = false,
    val isSearchActive: Boolean = false,
    val isThemePickerVisible: Boolean = false,
    val currentTheme: AppThemeMode = AppThemeMode.SYSTEM,
    val isTemplatePickerVisible: Boolean = false,
    val selectedTag: String? = null,
    val availableTags: List<String> = emptyList()
)

sealed interface NotesUiEvent {
    data class OnSearchQueryChanged(val query: String) : NotesUiEvent
    data class OnCategorySelected(val category: NoteCategory) : NotesUiEvent
    data class OnTagSelected(val tag: String?) : NotesUiEvent
    data class OnSortOrderChanged(val sortOrder: SortOrder) : NotesUiEvent
    data class OnTogglePin(val noteId: Long) : NotesUiEvent
    data class OnDeleteNote(val noteId: Long) : NotesUiEvent
    data class OnNoteClicked(val noteId: Long) : NotesUiEvent
    data object OnCreateNoteClicked : NotesUiEvent
    data object OnVaultClicked : NotesUiEvent
    data object OnTrashClicked : NotesUiEvent
    data object OnShowThemePicker : NotesUiEvent
    data object OnDismissThemePicker : NotesUiEvent
    data class OnThemeSelected(val theme: AppThemeMode) : NotesUiEvent
    data object OnShowTemplatePicker : NotesUiEvent
    data object OnDismissTemplatePicker : NotesUiEvent
    data class OnTemplateSelected(val template: NoteTemplate) : NotesUiEvent
    data object OnSettingsClicked : NotesUiEvent
}

sealed interface NotesUiEffect {
    data class NavigateToEditor(val noteId: Long) : NotesUiEffect
    data class NavigateToEditorWithTemplate(val template: NoteTemplate) : NotesUiEffect
    data object NavigateToVault : NotesUiEffect
    data object NavigateToTrash : NotesUiEffect
    data object NavigateToSettings : NotesUiEffect
    data class ShowSnackbar(val message: String) : NotesUiEffect
}

