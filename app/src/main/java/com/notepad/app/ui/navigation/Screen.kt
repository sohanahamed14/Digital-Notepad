package com.notepad.app.ui.navigation

sealed class Screen(val route: String) {
    data object NotesList : Screen("notes_list")
    data object NoteEditor : Screen("note_editor/{noteId}?template={template}") {
        fun createRoute(noteId: Long, template: String? = null): String {
            val base = "note_editor/$noteId"
            return if (template != null) "$base?template=$template" else base
        }
    }
    data object Vault : Screen("vault")
    data object Trash : Screen("trash")
    data object Settings : Screen("settings")
}

