package com.notepad.app.domain.usecase

import com.notepad.app.domain.repository.NoteRepository
import javax.inject.Inject

class ToggleVaultLockUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long, currentLockState: Boolean, content: String) {
        val newLockState = !currentLockState
        repository.toggleVaultLock(noteId, newLockState, content)
    }
}
