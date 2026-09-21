package com.notepad.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepad.app.core.backup.BackupManager
import com.notepad.app.core.backup.ImportResult
import com.notepad.app.core.security.BiometricAuthManager
import com.notepad.app.core.security.SecurityPreferences
import com.notepad.app.core.theme.AppThemeMode
import com.notepad.app.core.theme.ThemePreferences
import com.notepad.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

data class SettingsUiState(
    val currentTheme: AppThemeMode = AppThemeMode.SYSTEM,
    val isBiometricSupported: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val activeNotesCount: Int = 0,
    val vaultNotesCount: Int = 0,
    val trashNotesCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    private val securityPreferences: SecurityPreferences,
    private val biometricAuthManager: BiometricAuthManager,
    private val backupManager: BackupManager,
    private val noteRepository: NoteRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        themePreferences.themeMode,
        securityPreferences.isAppLockEnabled,
        noteRepository.getActiveNotes(),
        noteRepository.getVaultNotes(),
        noteRepository.getTrashNotes()
    ) { theme, appLock, active, vault, trash ->
        SettingsUiState(
            currentTheme = theme,
            isBiometricSupported = biometricAuthManager.canAuthenticate(),
            isAppLockEnabled = appLock,
            activeNotesCount = active.size,
            vaultNotesCount = vault.size,
            trashNotesCount = trash.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isBiometricSupported = biometricAuthManager.canAuthenticate())
    )

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch {
            themePreferences.setTheme(mode)
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setAppLockEnabled(enabled)
        }
    }

    fun exportBackup(outputStream: OutputStream, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = backupManager.exportNotesToJson(noteRepository, outputStream)
            onComplete(count)
        }
    }

    fun importBackup(inputStream: InputStream, onComplete: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.importNotesFromJson(noteRepository, inputStream)
            onComplete(result)
        }
    }

    fun exportEncryptedBackup(outputStream: OutputStream, passphrase: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = backupManager.exportEncryptedBackup(noteRepository, outputStream, passphrase)
            onComplete(count)
        }
    }

    fun importEncryptedBackup(inputStream: InputStream, passphrase: String, onComplete: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.importEncryptedBackup(noteRepository, inputStream, passphrase)
            onComplete(result)
        }
    }

    fun exportZipBackup(outputStream: OutputStream, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = backupManager.exportNotesToZip(noteRepository, outputStream)
            onComplete(count)
        }
    }

    fun importKeepOrTextFile(inputStream: InputStream, fileName: String, onComplete: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.importFromTextOrKeep(noteRepository, inputStream, fileName)
            onComplete(result)
        }
    }
}
