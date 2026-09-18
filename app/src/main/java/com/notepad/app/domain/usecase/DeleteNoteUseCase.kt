package com.notepad.app.domain.usecase

import com.notepad.app.domain.repository.NoteRepository
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend fun softDelete(id: Long) {
        repository.softDelete(id)
    }

    suspend fun restore(id: Long) {
        repository.restoreFromTrash(id)
    }

    suspend fun hardDelete(id: Long) {
        repository.hardDelete(id)
    }

    suspend fun clearTrash() {
        repository.clearTrash()
    }
}
