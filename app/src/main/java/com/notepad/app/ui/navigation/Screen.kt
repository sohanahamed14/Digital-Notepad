package com.notepad.app.ui.navigation

sealed class Screen(val route: String) {
    data object NotesList : Screen("notes_list")
    data object NoteEditor : Screen("note_editor/{noteId}?template={template}&voice={voice}") {
        fun createRoute(noteId: Long, template: String? = null, startVoice: Boolean = false): String {
            val base = "note_editor/$noteId"
            val params = mutableListOf<String>()
            if (!template.isNullOrEmpty()) params.add("template=$template")
            if (startVoice) params.add("voice=true")
            return if (params.isNotEmpty()) "$base?${params.joinToString("&")}" else base
        }
    }
    data object Vault : Screen("vault")
    data object Trash : Screen("trash")
    data object Settings : Screen("settings")
}

