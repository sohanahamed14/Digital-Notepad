package com.notepad.app

import com.notepad.app.core.backup.BackupManager
import com.notepad.app.core.security.BiometricAuthManager
import com.notepad.app.core.security.SecurityPreferences
import com.notepad.app.core.theme.AppThemeMode
import com.notepad.app.core.theme.ThemePreferences
import com.notepad.app.domain.model.Note
import com.notepad.app.domain.model.NoteVersion
import com.notepad.app.domain.repository.NoteRepository
import com.notepad.app.ui.navigation.Screen
import com.notepad.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsFeatureTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var themeFlow: MutableStateFlow<AppThemeMode>
    private lateinit var appLockFlow: MutableStateFlow<Boolean>
    private lateinit var fakeThemePreferences: ThemePreferences
    private lateinit var fakeSecurityPreferences: SecurityPreferences
    private lateinit var fakeBiometricAuthManager: BiometricAuthManager
    private lateinit var fakeBackupManager: BackupManager
    private lateinit var fakeRepository: NoteRepository
    private val savedNotesList = mutableListOf<Note>()
    private var recordedSetTheme: AppThemeMode? = null
    private var recordedSetAppLock: Boolean? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        themeFlow = MutableStateFlow(AppThemeMode.SYSTEM)
        appLockFlow = MutableStateFlow(false)
        recordedSetTheme = null
        recordedSetAppLock = null

        fakeThemePreferences = object : ThemePreferences() {
            override val themeMode: Flow<AppThemeMode> = themeFlow
            override suspend fun setTheme(mode: AppThemeMode) {
                recordedSetTheme = mode
                themeFlow.value = mode
            }
        }

        fakeSecurityPreferences = object : SecurityPreferences() {
            override val isAppLockEnabled: Flow<Boolean> = appLockFlow
            override suspend fun setAppLockEnabled(enabled: Boolean) {
                recordedSetAppLock = enabled
                appLockFlow.value = enabled
            }
        }

        fakeBiometricAuthManager = object : BiometricAuthManager() {
            override fun canAuthenticate(): Boolean = true
        }

        fakeBackupManager = BackupManager()
        savedNotesList.clear()

        fakeRepository = object : NoteRepository {
            override fun getActiveNotes(): Flow<List<Note>> = flowOf(listOf(Note(id = 1, title = "A"), Note(id = 2, title = "B")))
            override fun getVaultNotes(): Flow<List<Note>> = flowOf(listOf(Note(id = 3, title = "Vault Note", isVaultLocked = true)))
            override fun getTrashNotes(): Flow<List<Note>> = flowOf(listOf(Note(id = 4, title = "Trash Note", isDeleted = true)))
            override fun getNoteById(id: Long): Flow<Note?> = flowOf(null)
            override suspend fun getNoteByIdDirect(id: Long): Note? = null
            override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
            override suspend fun saveNote(note: Note, createSnapshot: Boolean): Long {
                savedNotesList.add(note)
                return 1L
            }
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSettingsRouteConfiguration() {
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun testSettingsViewModelLoadsStatisticsAndTheme() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(
            themePreferences = fakeThemePreferences,
            securityPreferences = fakeSecurityPreferences,
            biometricAuthManager = fakeBiometricAuthManager,
            backupManager = fakeBackupManager,
            noteRepository = fakeRepository
        )

        val job = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AppThemeMode.SYSTEM, state.currentTheme)
        assertTrue(state.isBiometricSupported)
        assertFalse(state.isAppLockEnabled)
        assertEquals(2, state.activeNotesCount)
        assertEquals(1, state.vaultNotesCount)
        assertEquals(1, state.trashNotesCount)
        job.cancel()
    }

    @Test
    fun testSettingsViewModelThemeChange() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(
            themePreferences = fakeThemePreferences,
            securityPreferences = fakeSecurityPreferences,
            biometricAuthManager = fakeBiometricAuthManager,
            backupManager = fakeBackupManager,
            noteRepository = fakeRepository
        )

        val job = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.setTheme(AppThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(AppThemeMode.DARK, recordedSetTheme)
        assertEquals(AppThemeMode.DARK, viewModel.uiState.value.currentTheme)
        job.cancel()
    }

    @Test
    fun testSettingsViewModelAppLockToggle() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(
            themePreferences = fakeThemePreferences,
            securityPreferences = fakeSecurityPreferences,
            biometricAuthManager = fakeBiometricAuthManager,
            backupManager = fakeBackupManager,
            noteRepository = fakeRepository
        )

        val job = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAppLockEnabled)

        viewModel.toggleAppLock(true)
        advanceUntilIdle()

        assertEquals(true, recordedSetAppLock)
        assertTrue(viewModel.uiState.value.isAppLockEnabled)

        viewModel.toggleAppLock(false)
        advanceUntilIdle()

        assertEquals(false, recordedSetAppLock)
        assertFalse(viewModel.uiState.value.isAppLockEnabled)
        job.cancel()
    }

    @Test
    fun testBackupExportAndImport() = runTest(testDispatcher) {
        val outStream = ByteArrayOutputStream()
        val count = fakeBackupManager.exportNotesToJson(fakeRepository, outStream)
        assertEquals(2, count)

        val exportedBytes = outStream.toByteArray()
        assertTrue(exportedBytes.isNotEmpty())

        val inStream = ByteArrayInputStream(exportedBytes)
        val importResult = fakeBackupManager.importNotesFromJson(fakeRepository, inStream)
        assertEquals(2, importResult.successCount)
        assertEquals(0, importResult.failureCount)
        assertEquals(2, savedNotesList.size)
        assertEquals("A", savedNotesList[0].title)
        assertEquals("B", savedNotesList[1].title)
    }

    @Test
    fun testEncryptedBackupExportAndImportSuccess() = runTest(testDispatcher) {
        val outStream = ByteArrayOutputStream()
        val passphrase = "MasterPassphrase!2026"
        val count = fakeBackupManager.exportEncryptedBackup(fakeRepository, outStream, passphrase)
        // 2 active + 1 vault note = 3
        assertEquals(3, count)

        val encryptedBytes = outStream.toByteArray()
        assertTrue(encryptedBytes.isNotEmpty())

        savedNotesList.clear()
        val inStream = ByteArrayInputStream(encryptedBytes)
        val result = fakeBackupManager.importEncryptedBackup(fakeRepository, inStream, passphrase)

        assertEquals(3, result.successCount)
        assertEquals(0, result.failureCount)
        assertEquals(3, savedNotesList.size)
        assertEquals("A", savedNotesList[0].title)
        assertEquals("Vault Note", savedNotesList[2].title)
        assertTrue(savedNotesList[2].isVaultLocked)
    }

    @Test
    fun testEncryptedBackupImportWrongPassword() = runTest(testDispatcher) {
        val outStream = ByteArrayOutputStream()
        val passphrase = "CorrectPassword123"
        fakeBackupManager.exportEncryptedBackup(fakeRepository, outStream, passphrase)

        val encryptedBytes = outStream.toByteArray()
        val inStream = ByteArrayInputStream(encryptedBytes)
        val result = fakeBackupManager.importEncryptedBackup(fakeRepository, inStream, "IncorrectPassword")

        assertEquals(0, result.successCount)
        assertTrue(result.errorMessage != null && result.errorMessage!!.contains("passphrase", ignoreCase = true))
    }

    @Test
    fun testZipBackupExport() = runTest(testDispatcher) {
        val outStream = ByteArrayOutputStream()
        val count = fakeBackupManager.exportNotesToZip(fakeRepository, outStream)
        assertEquals(2, count)

        val zipBytes = outStream.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        val zipIn = java.util.zip.ZipInputStream(ByteArrayInputStream(zipBytes))
        val entries = mutableListOf<String>()
        var entry = zipIn.nextEntry
        while (entry != null) {
            entries.add(entry.name)
            entry = zipIn.nextEntry
        }
        assertEquals(2, entries.size)
        assertTrue(entries.any { it.contains("1_A.md") })
        assertTrue(entries.any { it.contains("2_B.md") })
    }

    @Test
    fun testImportFromGoogleKeepJson() = runTest(testDispatcher) {
        savedNotesList.clear()
        val keepJson = """
            {
                "title": "Grocery Shopping",
                "textContent": "- Milk\n- Apples\n- Almond Flour",
                "isArchived": false
            }
        """.trimIndent()

        val inStream = ByteArrayInputStream(keepJson.toByteArray(Charsets.UTF_8))
        val result = fakeBackupManager.importFromTextOrKeep(fakeRepository, inStream, "shopping.json")

        assertEquals(1, result.successCount)
        assertEquals(1, savedNotesList.size)
        assertEquals("Grocery Shopping", savedNotesList[0].title)
        assertTrue(savedNotesList[0].content.contains("Milk"))
        assertEquals("KEEP_IMPORT", savedNotesList[0].category)
    }

    @Test
    fun testImportFromMarkdownFile() = runTest(testDispatcher) {
        savedNotesList.clear()
        val mdContent = """
            # Meeting Notes
            Discussed Q3 mobile roadmap and end-to-end cloud sync.
        """.trimIndent()

        val inStream = ByteArrayInputStream(mdContent.toByteArray(Charsets.UTF_8))
        val result = fakeBackupManager.importFromTextOrKeep(fakeRepository, inStream, "meeting.md")

        assertEquals(1, result.successCount)
        assertEquals(1, savedNotesList.size)
        assertEquals("Meeting Notes", savedNotesList[0].title)
        assertTrue(savedNotesList[0].content.contains("Discussed Q3"))
    }
}
