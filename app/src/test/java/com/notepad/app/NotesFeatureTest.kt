package com.notepad.app

import com.notepad.app.core.theme.AppThemeMode
import com.notepad.app.core.theme.ThemePreferences
import com.notepad.app.domain.model.Note
import com.notepad.app.domain.model.NoteCategory
import com.notepad.app.domain.model.NoteVersion
import com.notepad.app.domain.model.SortOrder
import com.notepad.app.domain.repository.NoteRepository
import com.notepad.app.domain.usecase.DeleteNoteUseCase
import com.notepad.app.domain.usecase.GetNotesUseCase
import com.notepad.app.domain.usecase.PurgeOldTrashUseCase
import com.notepad.app.domain.usecase.SearchNotesUseCase
import com.notepad.app.ui.notes.NotesUiEffect
import com.notepad.app.ui.notes.NotesUiEvent
import com.notepad.app.ui.notes.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesFeatureTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: NoteRepository
    private lateinit var fakeThemePreferences: ThemePreferences
    private val toggledPinIds = mutableListOf<Long>()
    private val deletedNoteIds = mutableListOf<Long>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        toggledPinIds.clear()
        deletedNoteIds.clear()

        fakeRepository = object : NoteRepository {
            override fun getActiveNotes(): Flow<List<Note>> = flowOf(listOf(Note(id = 10, title = "First Note")))
            override fun getVaultNotes(): Flow<List<Note>> = flowOf(emptyList())
            override fun getTrashNotes(): Flow<List<Note>> = flowOf(emptyList())
            override fun getNoteById(id: Long): Flow<Note?> = flowOf(null)
            override suspend fun getNoteByIdDirect(id: Long): Note? = null
            override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
            override suspend fun saveNote(note: Note, createSnapshot: Boolean): Long = 10L
            override suspend fun togglePin(id: Long) { toggledPinIds.add(id) }
            override suspend fun softDelete(id: Long) { deletedNoteIds.add(id) }
            override suspend fun restoreFromTrash(id: Long) {}
            override suspend fun hardDelete(id: Long) {}
            override suspend fun clearTrash() {}
            override suspend fun purgeOldTrash(daysThreshold: Int) {}
            override suspend fun toggleVaultLock(id: Long, isLocked: Boolean, newContent: String) {}
            override fun getNoteVersions(noteId: Long): Flow<List<NoteVersion>> = flowOf(emptyList())
            override suspend fun restoreNoteVersion(version: NoteVersion) {}
        }

        fakeThemePreferences = object : ThemePreferences() {
            override val themeMode: Flow<AppThemeMode> = flowOf(AppThemeMode.SYSTEM)
            override suspend fun setTheme(mode: AppThemeMode) {}
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): NotesViewModel {
        return NotesViewModel(
            getNotesUseCase = GetNotesUseCase(fakeRepository),
            searchNotesUseCase = SearchNotesUseCase(fakeRepository),
            deleteNoteUseCase = DeleteNoteUseCase(fakeRepository),
            purgeOldTrashUseCase = PurgeOldTrashUseCase(fakeRepository),
            repository = fakeRepository,
            themePreferences = fakeThemePreferences
        )
    }

    @Test
    fun testSettingsButtonClickedNavigatesToSettings() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectedEffects = mutableListOf<NotesUiEffect>()
        val job = launch {
            viewModel.uiEffect.collect { collectedEffects.add(it) }
        }

        viewModel.onEvent(NotesUiEvent.OnSettingsClicked)
        advanceUntilIdle()

        assertTrue(collectedEffects.contains(NotesUiEffect.NavigateToSettings))
        job.cancel()
    }

    @Test
    fun testNavigationEffects() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collectedEffects = mutableListOf<NotesUiEffect>()
        val job = launch {
            viewModel.uiEffect.collect { collectedEffects.add(it) }
        }

        viewModel.onEvent(NotesUiEvent.OnCreateNoteClicked)
        viewModel.onEvent(NotesUiEvent.OnVaultClicked)
        viewModel.onEvent(NotesUiEvent.OnTrashClicked)
        advanceUntilIdle()

        assertTrue(collectedEffects.contains(NotesUiEffect.NavigateToEditor(0L)))
        assertTrue(collectedEffects.contains(NotesUiEffect.NavigateToVault))
        assertTrue(collectedEffects.contains(NotesUiEffect.NavigateToTrash))
        job.cancel()
    }

    @Test
    fun testSearchAndCategoryFiltering() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(NotesUiEvent.OnSearchQueryChanged("Shopping"))
        assertEquals("Shopping", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.isSearchActive)

        viewModel.onEvent(NotesUiEvent.OnCategorySelected(NoteCategory.WORK))
        assertEquals(NoteCategory.WORK, viewModel.uiState.value.selectedCategory)

        viewModel.onEvent(NotesUiEvent.OnSortOrderChanged(SortOrder.TITLE_ASC))
        assertEquals(SortOrder.TITLE_ASC, viewModel.uiState.value.sortOrder)
    }

    @Test
    fun testPinAndSoftDeleteDelegation() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(NotesUiEvent.OnTogglePin(42L))
        viewModel.onEvent(NotesUiEvent.OnDeleteNote(99L))
        advanceUntilIdle()

        assertTrue(toggledPinIds.contains(42L))
        assertTrue(deletedNoteIds.contains(99L))
    }

    @Test
    fun testNoteTagExtractionAndFiltering() = runTest(testDispatcher) {
        val noteWithTags = Note(id = 1, title = "Meeting #work", content = "Project updates #work #q3")
        assertEquals(listOf("work", "q3"), noteWithTags.tags)

        val viewModel = createViewModel()
        viewModel.onEvent(NotesUiEvent.OnTagSelected("work"))
        assertEquals("work", viewModel.uiState.value.selectedTag)

        // Toggle tag off
        viewModel.onEvent(NotesUiEvent.OnTagSelected("work"))
        assertNull(viewModel.uiState.value.selectedTag)
    }
}
