package com.notepad.app.ui.vault

import com.notepad.app.domain.model.Note

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface VaultUiEvent {
    data object OnAuthenticateClicked : VaultUiEvent
    data object OnAuthenticationSucceeded : VaultUiEvent
    data object OnLockVault : VaultUiEvent
    data class OnNoteClicked(val noteId: Long) : VaultUiEvent
    data class OnRemoveFromVault(val note: Note) : VaultUiEvent
}

sealed interface VaultUiEffect {
    data class NavigateToEditor(val noteId: Long) : VaultUiEffect
    data object RequestBiometric : VaultUiEffect
    data class ShowSnackbar(val message: String) : VaultUiEffect
}
