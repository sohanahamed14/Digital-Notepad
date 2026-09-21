package com.notepad.app.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepad.app.core.media.AttachmentManager
import com.notepad.app.core.security.CryptoManager
import com.notepad.app.core.speech.VoiceToTextManager
import com.notepad.app.core.template.NoteTemplate
import com.notepad.app.domain.model.Note
import com.notepad.app.domain.model.NoteVersion
import com.notepad.app.domain.usecase.GetNoteByIdUseCase
import com.notepad.app.domain.usecase.RestoreVersionUseCase
import com.notepad.app.domain.usecase.SaveNoteUseCase
import com.notepad.app.domain.usecase.ToggleVaultLockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val restoreVersionUseCase: RestoreVersionUseCase,
    private val toggleVaultLockUseCase: ToggleVaultLockUseCase,
    private val cryptoManager: CryptoManager,
    private val voiceToTextManager: VoiceToTextManager,
    private val attachmentManager: AttachmentManager,
    private val pdfExportManager: com.notepad.app.core.export.PdfExportManager,
    private val noteInsightsManager: com.notepad.app.core.insights.NoteInsightsManager,
    private val reminderManager: com.notepad.app.core.reminder.ReminderManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val templateName: String? = savedStateHandle.get<String>("template")

    private val _uiState = MutableStateFlow(NoteEditorUiState(id = noteId))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<NoteEditorUiEffect>()
    val uiEffect: SharedFlow<NoteEditorUiEffect> = _uiEffect.asSharedFlow()

    init {
        if (noteId != 0L) {
            viewModelScope.launch {
                val existingNote = getNoteByIdUseCase.direct(noteId)
                if (existingNote != null) {
                    val decryptedContent = if (existingNote.isVaultLocked) {
                        cryptoManager.decryptString(existingNote.content)
                    } else {
                        existingNote.content
                    }

                    _uiState.update {
                        it.copy(
                            id = existingNote.id,
                            title = existingNote.title,
                            content = decryptedContent,
                            colorHex = existingNote.colorHex,
                            category = existingNote.category,
                            isPinned = existingNote.isPinned,
                            isVaultLocked = existingNote.isVaultLocked,
                            reminderAt = existingNote.reminderAt,
                            wordCount = calculateWords(decryptedContent),
                            characterCount = decryptedContent.length
                        )
                    }
                }
            }

            // Observe Version Snapshots
            restoreVersionUseCase.getVersions(noteId).onEach { versions ->
                _uiState.update { it.copy(versions = versions) }
            }.launchIn(viewModelScope)
        } else if (!templateName.isNullOrBlank()) {
            // Pre-fill from template for new notes
            val template = try { NoteTemplate.valueOf(templateName) } catch (_: Exception) { null }
            if (template != null && template != NoteTemplate.BLANK) {
                val resolvedContent = NoteTemplate.resolveContent(template)
                _uiState.update {
                    it.copy(
                        title = template.templateTitle,
                        content = resolvedContent,
                        category = template.category.name,
                        wordCount = calculateWords(resolvedContent),
                        characterCount = resolvedContent.length
                    )
                }
            }
        }

        // Observe speech recognition updates
        voiceToTextManager.state.onEach { voiceState ->
            _uiState.update { it.copy(isListeningVoice = voiceState.isListening) }
            if (voiceState.spokenText.isNotBlank() && !voiceState.isListening) {
                appendSpokenText(voiceState.spokenText)
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: NoteEditorUiEvent) {
        when (event) {
            is NoteEditorUiEvent.OnTitleChanged -> {
                _uiState.update { it.copy(title = event.title) }
            }
            is NoteEditorUiEvent.OnContentChanged -> {
                val words = calculateWords(event.content)
                _uiState.update {
                    it.copy(
                        content = event.content,
                        wordCount = words,
                        characterCount = event.content.length
                    )
                }
            }
            is NoteEditorUiEvent.OnCategoryChanged -> {
                _uiState.update { it.copy(category = event.category) }
            }
            is NoteEditorUiEvent.OnTextColorChanged -> {
                _uiState.update { it.copy(colorHex = event.colorHex) }
            }
            is NoteEditorUiEvent.OnTogglePin -> {
                _uiState.update { it.copy(isPinned = !it.isPinned) }
            }
            is NoteEditorUiEvent.OnToggleMarkdownPreview -> {
                _uiState.update { it.copy(isMarkdownPreview = !it.isMarkdownPreview) }
            }
            is NoteEditorUiEvent.OnFormatText -> {
                applyMarkdownFormat(event.action)
            }
            is NoteEditorUiEvent.OnToggleVoiceInput -> {
                if (_uiState.value.isListeningVoice) {
                    voiceToTextManager.stopListening()
                } else {
                    voiceToTextManager.startListening()
                }
            }
            is NoteEditorUiEvent.OnToggleVaultLock -> {
                val targetLock = !_uiState.value.isVaultLocked
                viewModelScope.launch {
                    _uiEffect.emit(NoteEditorUiEffect.RequestBiometric {
                        _uiState.update { it.copy(isVaultLocked = targetLock) }
                    })
                }
            }
            is NoteEditorUiEvent.OnSaveNote -> {
                saveNoteAndExit()
            }
            is NoteEditorUiEvent.OnShowVersionHistory -> {
                _uiState.update { it.copy(isVersionSheetVisible = true) }
            }
            is NoteEditorUiEvent.OnDismissVersionHistory -> {
                _uiState.update { it.copy(isVersionSheetVisible = false) }
            }
            is NoteEditorUiEvent.OnRestoreVersion -> {
                restoreVersion(event.version)
            }
            is NoteEditorUiEvent.OnCheckboxToggled -> {
                handleCheckboxToggle(event.lineIndex, event.newChecked)
            }
            is NoteEditorUiEvent.OnShowInsights -> {
                val insights = noteInsightsManager.analyze(_uiState.value.content)
                _uiState.update {
                    it.copy(
                        isInsightsSheetVisible = true,
                        readingTimeMinutes = insights.readingTimeMinutes,
                        extractedTasks = insights.extractedTasks,
                        keyTakeaways = insights.keyTakeaways
                    )
                }
            }
            is NoteEditorUiEvent.OnDismissInsights -> {
                _uiState.update { it.copy(isInsightsSheetVisible = false) }
            }
            is NoteEditorUiEvent.OnAppendExtractedTasks -> {
                val updatedContent = noteInsightsManager.appendExtractedTasksAsChecklist(
                    _uiState.value.content,
                    _uiState.value.extractedTasks
                )
                onEvent(NoteEditorUiEvent.OnContentChanged(updatedContent))
                _uiState.update { it.copy(isInsightsSheetVisible = false) }
            }
            is NoteEditorUiEvent.OnShowReminderDialog -> {
                _uiState.update { it.copy(isReminderDialogVisible = true) }
            }
            is NoteEditorUiEvent.OnDismissReminderDialog -> {
                _uiState.update { it.copy(isReminderDialogVisible = false) }
            }
            is NoteEditorUiEvent.OnSetReminder -> {
                _uiState.update { it.copy(reminderAt = event.timestampMillis, isReminderDialogVisible = false) }
                viewModelScope.launch {
                    _uiEffect.emit(NoteEditorUiEffect.ShowSnackbar("Reminder scheduled"))
                }
            }
            is NoteEditorUiEvent.OnClearReminder -> {
                if (_uiState.value.id != 0L) {
                    reminderManager.cancelReminder(_uiState.value.id)
                }
                _uiState.update { it.copy(reminderAt = null, isReminderDialogVisible = false) }
            }
            is NoteEditorUiEvent.OnExportPdf -> {
                val note = buildCurrentNote()
                val intent = pdfExportManager.sharePdf(note)
                viewModelScope.launch {
                    _uiEffect.emit(NoteEditorUiEffect.LaunchShareIntent(intent))
                }
            }
            is NoteEditorUiEvent.OnShareMarkdown -> {
                val note = buildCurrentNote()
                val intent = pdfExportManager.shareMarkdown(note)
                viewModelScope.launch {
                    _uiEffect.emit(NoteEditorUiEffect.LaunchShareIntent(intent))
                }
            }
        }
    }

    private fun buildCurrentNote(): Note {
        val state = _uiState.value
        return Note(
            id = state.id,
            title = state.title.ifBlank { "Untitled Note" },
            content = state.content,
            colorHex = state.colorHex,
            category = state.category,
            isPinned = state.isPinned,
            isVaultLocked = state.isVaultLocked,
            reminderAt = state.reminderAt
        )
    }

    private fun applyMarkdownFormat(action: MarkdownAction) {
        val currentContent = _uiState.value.content
        val formatted = when (action) {
            MarkdownAction.BOLD -> if (currentContent.isEmpty()) "****" else "$currentContent **bold**"
            MarkdownAction.ITALIC -> if (currentContent.isEmpty()) "**" else "$currentContent *italic*"
            MarkdownAction.HEADER -> if (currentContent.isEmpty()) "# " else "$currentContent\n# Header"
            MarkdownAction.CODE_BLOCK -> if (currentContent.isEmpty()) "```\n\n```" else "$currentContent\n```\ncode\n```"
            MarkdownAction.CHECKLIST -> if (currentContent.isEmpty()) "- [ ] " else "$currentContent\n- [ ] Task"
            MarkdownAction.BULLET_LIST -> if (currentContent.isEmpty()) "- " else "$currentContent\n- Item"
            MarkdownAction.TAG -> if (currentContent.isEmpty() || currentContent.endsWith(" ") || currentContent.endsWith("\n")) "${currentContent}#" else "$currentContent #"
        }
        onEvent(NoteEditorUiEvent.OnContentChanged(formatted))
    }

    private fun handleCheckboxToggle(lineIndex: Int, newChecked: Boolean) {
        val lines = _uiState.value.content.lines().toMutableList()
        if (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            val updatedLine = if (newChecked) {
                line.replace("- [ ] ", "- [x] ").replace("* [ ] ", "* [x] ")
            } else {
                line.replace("- [x] ", "- [ ] ").replace("- [X] ", "- [ ] ")
                    .replace("* [x] ", "* [ ] ").replace("* [X] ", "* [ ] ")
            }
            lines[lineIndex] = updatedLine
            val newContent = lines.joinToString("\n")
            onEvent(NoteEditorUiEvent.OnContentChanged(newContent))
        }
    }

    private fun appendSpokenText(spoken: String) {
        val current = _uiState.value.content
        val newContent = if (current.isBlank()) spoken else "$current $spoken"
        onEvent(NoteEditorUiEvent.OnContentChanged(newContent))
    }

    private fun restoreVersion(version: NoteVersion) {
        _uiState.update {
            it.copy(
                title = version.title,
                content = version.content,
                wordCount = calculateWords(version.content),
                characterCount = version.content.length,
                isVersionSheetVisible = false
            )
        }
        viewModelScope.launch {
            _uiEffect.emit(NoteEditorUiEffect.ShowSnackbar("Restored version from ${version.savedAt}"))
        }
    }

    private fun saveNoteAndExit() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSaving = true) }

            val note = Note(
                id = state.id,
                title = state.title,
                content = state.content,
                colorHex = state.colorHex,
                category = state.category,
                isPinned = state.isPinned,
                isVaultLocked = state.isVaultLocked,
                reminderAt = state.reminderAt
            )

            val savedId = saveNoteUseCase(note)
            val noteWithId = note.copy(id = if (note.id == 0L) savedId else note.id)

            // Schedule reminder if configured
            if (state.reminderAt != null && state.reminderAt > System.currentTimeMillis()) {
                reminderManager.scheduleReminder(noteWithId, state.reminderAt)
            }

            _uiState.update { it.copy(isSaving = false) }
            _uiEffect.emit(NoteEditorUiEffect.NavigateBack)
        }
    }

    private fun calculateWords(text: String): Int {
        return if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
    }

    fun attachImage(uri: Uri) {
        viewModelScope.launch {
            val path = attachmentManager.saveImageFromUri(uri)
            if (path != null) {
                val current = _uiState.value.content
                val updated = if (current.isBlank()) "![Image](file://$path)" else "$current\n\n![Image](file://$path)"
                onEvent(NoteEditorUiEvent.OnContentChanged(updated))
            }
        }
    }

    fun attachSketch(bitmap: Bitmap) {
        viewModelScope.launch {
            val path = attachmentManager.saveSketchBitmap(bitmap)
            if (path != null) {
                val current = _uiState.value.content
                val updated = if (current.isBlank()) "![Sketch](file://$path)" else "$current\n\n![Sketch](file://$path)"
                onEvent(NoteEditorUiEvent.OnContentChanged(updated))
            }
        }
    }

    fun toggleAudioRecording() {
        if (_uiState.value.isRecordingAudio) {
            val path = attachmentManager.stopVoiceRecording()
            _uiState.update { it.copy(isRecordingAudio = false) }
            if (path != null) {
                val current = _uiState.value.content
                val updated = if (current.isBlank()) "🎙 [Voice Memo: $path]" else "$current\n\n🎙 [Voice Memo: $path]"
                onEvent(NoteEditorUiEvent.OnContentChanged(updated))
            }
        } else {
            val path = attachmentManager.startVoiceRecording()
            if (path != null) {
                _uiState.update { it.copy(isRecordingAudio = true) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceToTextManager.stopListening()
        attachmentManager.stopVoiceRecording()
        attachmentManager.stopAudio()
    }
}
