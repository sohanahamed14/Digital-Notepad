package com.notepad.app.domain.usecase

import com.notepad.app.domain.model.NoteVersion
import com.notepad.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RestoreVersionUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    fun getVersions(noteId: Long): Flow<List<NoteVersion>> {
        return repository.getNoteVersions(noteId)
    }

    suspend fun restore(version: NoteVersion) {
        repository.restoreNoteVersion(version)
    }
}
