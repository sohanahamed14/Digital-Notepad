package com.notepad.app.domain.usecase

import com.notepad.app.domain.model.Note
import com.notepad.app.domain.repository.NoteRepository
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note, createSnapshot: Boolean = true): Long {
        val sanitizedTitle = note.title.trim()
        val sanitizedContent = note.content.trim()

        if (sanitizedTitle.isBlank() && sanitizedContent.isBlank()) {
            return 0L
        }

        return repository.saveNote(
            note.copy(
                title = sanitizedTitle.ifBlank { "Untitled Note" },
                content = sanitizedContent,
                updatedAt = System.currentTimeMillis()
            ),
            createSnapshot = createSnapshot
        )
    }
}
