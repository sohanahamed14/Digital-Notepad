package com.notepad.app.data.repository

import com.notepad.app.core.security.CryptoManager
import com.notepad.app.data.local.dao.NoteDao
import com.notepad.app.data.local.dao.NoteVersionDao
import com.notepad.app.data.local.entity.NoteEntity
import com.notepad.app.data.local.entity.NoteVersionEntity
import com.notepad.app.domain.model.Note
import com.notepad.app.domain.model.NoteVersion
import com.notepad.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val noteVersionDao: NoteVersionDao,
    private val cryptoManager: CryptoManager
) : NoteRepository {

    override fun getActiveNotes(): Flow<List<Note>> {
        return noteDao.getActiveNotes().map { list -> list.map { it.toDomain() } }
    }

    override fun getVaultNotes(): Flow<List<Note>> {
        return noteDao.getVaultNotes().map { list -> list.map { it.toDomain() } }
    }

    override fun getTrashNotes(): Flow<List<Note>> {
        return noteDao.getTrashNotes().map { list -> list.map { it.toDomain() } }
    }

    override fun getNoteById(id: Long): Flow<Note?> {
        return noteDao.getNoteByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getNoteByIdDirect(id: Long): Note? {
        return noteDao.getNoteById(id)?.toDomain()
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getActiveNotes()

        // Sanitize for FTS MATCH query, append wildcard for prefix match
        val sanitizedFts = trimmed.replace("\"", "").trim()
        return if (sanitizedFts.isNotEmpty()) {
            val ftsQuery = "\"$sanitizedFts\"*"
            noteDao.searchNotesFts(ftsQuery)
                .map { list -> list.map { it.toDomain() } }
                .catch { emitAll(noteDao.searchNotesFallback(trimmed).map { list -> list.map { it.toDomain() } }) }
        } else {
            noteDao.searchNotesFallback(trimmed).map { list -> list.map { it.toDomain() } }
        }
    }

    override suspend fun saveNote(note: Note, createSnapshot: Boolean): Long {
        val now = System.currentTimeMillis()
        val existing = if (note.id != 0L) noteDao.getNoteById(note.id) else null

        // If locked, encrypt content before saving
        val storedContent = if (note.isVaultLocked) {
            cryptoManager.encryptString(note.content)
        } else {
            note.content
        }

        val entityToSave = note.toEntity().copy(
            content = storedContent,
            updatedAt = now,
            createdAt = existing?.createdAt ?: now
        )

        val savedId = noteDao.insertNote(entityToSave)
        val finalId = if (note.id == 0L) savedId else note.id

        // Record snapshot if editing and content changed (only for unencrypted notes or plaintext snapshot)
        if (createSnapshot && existing != null && existing.content != storedContent && !note.isVaultLocked) {
            noteVersionDao.recordSnapshot(
                noteId = finalId,
                title = existing.title,
                content = existing.content
            )
        }

        return finalId
    }

    override suspend fun togglePin(id: Long) {
        noteDao.togglePin(id)
    }

    override suspend fun softDelete(id: Long) {
        noteDao.softDelete(id)
    }

    override suspend fun restoreFromTrash(id: Long) {
        noteDao.restoreFromTrash(id)
    }

    override suspend fun hardDelete(id: Long) {
        noteVersionDao.deleteAllVersionsForNote(id)
        noteDao.hardDelete(id)
    }

    override suspend fun clearTrash() {
        noteDao.clearTrash()
    }

    override suspend fun purgeOldTrash(daysThreshold: Int) {
        val cutoff = System.currentTimeMillis() - (daysThreshold.toLong() * 24L * 60L * 60L * 1000L)
        noteDao.purgeOldTrash(cutoff)
    }

    override suspend fun toggleVaultLock(id: Long, isLocked: Boolean, newContent: String) {
        if (isLocked) {
            // Locking: encrypt the plaintext content
            val encrypted = cryptoManager.encryptString(newContent)
            noteDao.updateVaultLock(id, isLocked, encrypted)
        } else {
            // Unlocking: fetch raw encrypted content from DB and decrypt it
            val entity = noteDao.getNoteById(id) ?: return
            val decrypted = cryptoManager.decryptString(entity.content)
            noteDao.updateVaultLock(id, isLocked, decrypted)
        }
    }

    override fun getNoteVersions(noteId: Long): Flow<List<NoteVersion>> {
        return noteVersionDao.getVersionsForNote(noteId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun restoreNoteVersion(version: NoteVersion) {
        val current = noteDao.getNoteById(version.noteId) ?: return
        // Take snapshot of current state before reverting
        noteVersionDao.recordSnapshot(
            noteId = current.id,
            title = current.title,
            content = current.content
        )
        // Revert to snapshot
        val updated = current.copy(
            title = version.title,
            content = version.content,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updated)
    }

    // Mapper extensions
    private fun NoteEntity.toDomain(): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            colorHex = colorHex,
            category = category,
            isPinned = isPinned,
            isVaultLocked = isVaultLocked,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            reminderAt = reminderAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Note.toEntity(): NoteEntity {
        return NoteEntity(
            id = id,
            title = title,
            content = content,
            colorHex = colorHex,
            category = category,
            isPinned = isPinned,
            isVaultLocked = isVaultLocked,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            reminderAt = reminderAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun NoteVersionEntity.toDomain(): NoteVersion {
        return NoteVersion(
            versionId = versionId,
            noteId = noteId,
            title = title,
            content = content,
            savedAt = savedAt
        )
    }
}
