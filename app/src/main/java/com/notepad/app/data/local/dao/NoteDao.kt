package com.notepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notepad.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isVaultLocked = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isVaultLocked = 1 ORDER BY updatedAt DESC")
    fun getVaultNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteByIdFlow(id: Long): Flow<NoteEntity?>

    @Query("""
        SELECT n.* FROM notes n
        JOIN notes_fts fts ON n.id = fts.rowid
        WHERE notes_fts MATCH :query
        AND n.isDeleted = 0 AND n.isVaultLocked = 0
        ORDER BY n.isPinned DESC, n.updatedAt DESC
    """)
    fun searchNotesFts(query: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND isVaultLocked = 0
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotesFallback(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePin(id: Long)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun clearTrash()

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :purgeThreshold")
    suspend fun purgeOldTrash(purgeThreshold: Long)

    @Query("UPDATE notes SET isVaultLocked = :isLocked, content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateVaultLock(id: Long, isLocked: Boolean, content: String, updatedAt: Long = System.currentTimeMillis())
}
