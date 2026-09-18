package com.notepad.app.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepad.app.core.security.CryptoManager
import com.notepad.app.domain.model.Note
import com.notepad.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<VaultUiEffect>()
    val uiEffect: SharedFlow<VaultUiEffect> = _uiEffect.asSharedFlow()

    init {
        // Auto trigger biometric prompt on entry
        viewModelScope.launch {
            _uiEffect.emit(VaultUiEffect.RequestBiometric)
        }
    }

    fun onEvent(event: VaultUiEvent) {
        when (event) {
            is VaultUiEvent.OnAuthenticateClicked -> {
                viewModelScope.launch {
                    _uiEffect.emit(VaultUiEffect.RequestBiometric)
                }
            }
            is VaultUiEvent.OnAuthenticationSucceeded -> {
                _uiState.update { it.copy(isUnlocked = true, isLoading = true) }
                observeVaultNotes()
            }
            is VaultUiEvent.OnLockVault -> {
                _uiState.update { it.copy(isUnlocked = false, notes = emptyList()) }
            }
            is VaultUiEvent.OnNoteClicked -> {
                viewModelScope.launch {
                    _uiEffect.emit(VaultUiEffect.NavigateToEditor(event.noteId))
                }
            }
            is VaultUiEvent.OnRemoveFromVault -> {
                viewModelScope.launch {
                    repository.toggleVaultLock(event.note.id, isLocked = false, event.note.content)
                    _uiEffect.emit(VaultUiEffect.ShowSnackbar("Note removed from Vault"))
                }
            }
        }
    }

    private fun observeVaultNotes() {
        repository.getVaultNotes().onEach { list ->
            // In-memory decryption only
            val decryptedList = list.map { note ->
                val plaintext = cryptoManager.decryptString(note.content)
                note.copy(content = plaintext)
            }
            _uiState.update { it.copy(notes = decryptedList, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        // Clear decrypted content from memory
        _uiState.value = VaultUiState()
    }
}
