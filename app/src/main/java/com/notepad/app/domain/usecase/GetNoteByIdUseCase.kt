package com.notepad.app.domain.usecase

import com.notepad.app.domain.model.Note
import com.notepad.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNoteByIdUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(id: Long): Flow<Note?> {
        return repository.getNoteById(id)
    }

    suspend fun direct(id: Long): Note? {
        return repository.getNoteByIdDirect(id)
    }
}
