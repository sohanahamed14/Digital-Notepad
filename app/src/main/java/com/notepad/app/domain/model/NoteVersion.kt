package com.notepad.app.domain.model

data class NoteVersion(
    val versionId: Long = 0,
    val noteId: Long,
    val title: String,
    val content: String,
    val savedAt: Long
)
