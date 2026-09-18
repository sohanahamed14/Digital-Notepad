package com.notepad.app

import com.notepad.app.domain.model.Note
import com.notepad.app.domain.model.NoteCategory
import com.notepad.app.domain.model.NoteVersion
import com.notepad.app.domain.model.SortOrder
import com.notepad.app.domain.repository.NoteRepository
import com.notepad.app.domain.usecase.GetNotesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetNotesUseCaseTest {

    private lateinit var fakeRepository: NoteRepository
    private lateinit var getNotesUseCase: GetNotesUseCase

    @Before
    fun setUp() {
        val testNotes = listOf(
            Note(id = 1, title = "Alpha Note", category = NoteCategory.WORK.name, updatedAt = 1000L, isPinned = false),
            Note(id = 2, title = "Beta Note", category = NoteCategory.PERSONAL.name, updatedAt = 2000L, isPinned = true),
            Note(id = 3, title = "Gamma Note", category = NoteCategory.WORK.name, updatedAt = 3000L, isPinned = false)
        )

        fakeRepository = object : NoteRepository {
            override fun getActiveNotes(): Flow<List<Note>> = flowOf(testNotes)
            override fun getVaultNotes(): Flow<List<Note>> = flowOf(emptyList())
            override fun getTrashNotes(): Flow<List<Note>> = flowOf(emptyList())
            override fun getNoteById(id: Long): Flow<Note?> = flowOf(null)
            override suspend fun getNoteByIdDirect(id: Long): Note? = null
            override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
            override suspend fun saveNote(note: Note, createSnapshot: Boolean): Long = 1L
            override suspend fun togglePin(id: Long) {}
            override suspend fun softDelete(id: Long) {}
            override suspend fun restoreFromTrash(id: Long) {}
            override suspend fun hardDelete(id: Long) {}
            override suspend fun clearTrash() {}
            override suspend fun purgeOldTrash(daysThreshold: Int) {}
            override suspend fun toggleVaultLock(id: Long, isLocked: Boolean, newContent: String) {}
            override fun getNoteVersions(noteId: Long): Flow<List<NoteVersion>> = flowOf(emptyList())
            override suspend fun restoreNoteVersion(version: NoteVersion) {}
        }

        getNotesUseCase = GetNotesUseCase(fakeRepository)
    }

    @Test
    fun testFilteringByCategory() = runBlocking {
        val workNotes = getNotesUseCase(category = NoteCategory.WORK).first()
        assertEquals(2, workNotes.size)
        assertEquals("Gamma Note", workNotes[0].title)
        assertEquals("Alpha Note", workNotes[1].title)
    }

    @Test
    fun testPinnedNotesAlwaysFirst() = runBlocking {
        val allNotes = getNotesUseCase(category = NoteCategory.ALL).first()
        // Beta Note is pinned, so it must be first even if Gamma has higher updatedAt
        assertEquals("Beta Note", allNotes[0].title)
    }

    @Test
    fun testSortByTitleAsc() = runBlocking {
        val sortedNotes = getNotesUseCase(
            category = NoteCategory.ALL,
            sortOrder = SortOrder.TITLE_ASC
        ).first()

        // Pinned note comes first
        assertEquals("Beta Note", sortedNotes[0].title)
        assertEquals("Alpha Note", sortedNotes[1].title)
        assertEquals("Gamma Note", sortedNotes[2].title)
    }
}
