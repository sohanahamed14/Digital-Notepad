package com.notepad.app.domain.repository

import com.notepad.app.domain.model.Note
import com.notepad.app.domain.model.NoteVersion
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getActiveNotes(): Flow<List<Note>>
    fun getVaultNotes(): Flow<List<Note>>
    fun getTrashNotes(): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    suspend fun getNoteByIdDirect(id: Long): Note?
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun saveNote(note: Note, createSnapshot: Boolean = true): Long
    suspend fun togglePin(id: Long)
    suspend fun softDelete(id: Long)
    suspend fun restoreFromTrash(id: Long)
    suspend fun hardDelete(id: Long)
    suspend fun clearTrash()
    suspend fun purgeOldTrash(daysThreshold: Int = 30)
    suspend fun toggleVaultLock(id: Long, isLocked: Boolean, newContent: String)
    fun getNoteVersions(noteId: Long): Flow<List<NoteVersion>>
    suspend fun restoreNoteVersion(version: NoteVersion)
}
