package com.notepad.app.domain.usecase

import com.notepad.app.domain.model.Note
import com.notepad.app.domain.model.NoteCategory
import com.notepad.app.domain.model.SortOrder
import com.notepad.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(
        category: NoteCategory = NoteCategory.ALL,
        sortOrder: SortOrder = SortOrder.DATE_MODIFIED_DESC
    ): Flow<List<Note>> {
        return repository.getActiveNotes().map { notes ->
            val filtered = if (category == NoteCategory.ALL) {
                notes
            } else {
                notes.filter { it.category.equals(category.name, ignoreCase = true) }
            }

            when (sortOrder) {
                SortOrder.DATE_MODIFIED_DESC -> filtered.sortedWith(
                    compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt }
                )
                SortOrder.DATE_MODIFIED_ASC -> filtered.sortedWith(
                    compareByDescending<Note> { it.isPinned }.thenBy { it.updatedAt }
                )
                SortOrder.TITLE_ASC -> filtered.sortedWith(
                    compareByDescending<Note> { it.isPinned }.thenBy { it.title.lowercase() }
                )
                SortOrder.TITLE_DESC -> filtered.sortedWith(
                    compareByDescending<Note> { it.isPinned }.thenByDescending { it.title.lowercase() }
                )
            }
        }
    }
}
