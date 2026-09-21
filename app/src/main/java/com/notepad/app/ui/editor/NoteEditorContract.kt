package com.notepad.app.ui.editor

import com.notepad.app.core.theme.TextColorDefault
import com.notepad.app.domain.model.NoteCategory
import com.notepad.app.domain.model.NoteVersion

enum class MarkdownAction {
    BOLD,
    ITALIC,
    HEADER,
    CODE_BLOCK,
    CHECKLIST,
    BULLET_LIST,
    TAG
}

data class NoteEditorUiState(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val colorHex: Long = TextColorDefault,
    val category: String = NoteCategory.GENERAL.name,
    val isPinned: Boolean = false,
    val isVaultLocked: Boolean = false,
    val isMarkdownPreview: Boolean = false,
    val isListeningVoice: Boolean = false,
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    val versions: List<NoteVersion> = emptyList(),
    val isVersionSheetVisible: Boolean = false,
    val isSaving: Boolean = false,
    val reminderAt: Long? = null,
    val isInsightsSheetVisible: Boolean = false,
    val isReminderDialogVisible: Boolean = false,
    val readingTimeMinutes: Int = 1,
    val extractedTasks: List<String> = emptyList(),
    val keyTakeaways: List<String> = emptyList(),
    val isRecordingAudio: Boolean = false
)

sealed interface NoteEditorUiEvent {
    data class OnTitleChanged(val title: String) : NoteEditorUiEvent
    data class OnContentChanged(val content: String) : NoteEditorUiEvent
    data class OnCategoryChanged(val category: String) : NoteEditorUiEvent
    data class OnTextColorChanged(val colorHex: Long) : NoteEditorUiEvent
    data object OnTogglePin : NoteEditorUiEvent
    data object OnToggleMarkdownPreview : NoteEditorUiEvent
    data class OnFormatText(val action: MarkdownAction) : NoteEditorUiEvent
    data object OnToggleVoiceInput : NoteEditorUiEvent
    data object OnToggleVaultLock : NoteEditorUiEvent
    data object OnSaveNote : NoteEditorUiEvent
    data object OnShowVersionHistory : NoteEditorUiEvent
    data object OnDismissVersionHistory : NoteEditorUiEvent
    data class OnRestoreVersion(val version: NoteVersion) : NoteEditorUiEvent
    data class OnCheckboxToggled(val lineIndex: Int, val newChecked: Boolean) : NoteEditorUiEvent
    data object OnShowInsights : NoteEditorUiEvent
    data object OnDismissInsights : NoteEditorUiEvent
    data object OnAppendExtractedTasks : NoteEditorUiEvent
    data object OnShowReminderDialog : NoteEditorUiEvent
    data object OnDismissReminderDialog : NoteEditorUiEvent
    data class OnSetReminder(val timestampMillis: Long) : NoteEditorUiEvent
    data object OnClearReminder : NoteEditorUiEvent
    data object OnExportPdf : NoteEditorUiEvent
    data object OnShareMarkdown : NoteEditorUiEvent
}

sealed interface NoteEditorUiEffect {
    data object NavigateBack : NoteEditorUiEffect
    data class ShowSnackbar(val message: String) : NoteEditorUiEffect
    data class RequestBiometric(val onAuthenticated: () -> Unit) : NoteEditorUiEffect
    data class LaunchShareIntent(val intent: android.content.Intent) : NoteEditorUiEffect
}
