package com.notepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.notepad.app.data.local.entity.NoteVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteVersionDao {

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY savedAt DESC")
    fun getVersionsForNote(noteId: Long): Flow<List<NoteVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: NoteVersionEntity): Long

    @Query("""
        DELETE FROM note_versions 
        WHERE noteId = :noteId 
        AND versionId NOT IN (
            SELECT versionId FROM note_versions 
            WHERE noteId = :noteId 
            ORDER BY savedAt DESC 
            LIMIT :maxVersions
        )
    """)
    suspend fun pruneOldVersions(noteId: Long, maxVersions: Int = 5)

    @Transaction
    suspend fun recordSnapshot(noteId: Long, title: String, content: String) {
        insertVersion(NoteVersionEntity(noteId = noteId, title = title, content = content))
        pruneOldVersions(noteId, 5)
    }

    @Query("DELETE FROM note_versions WHERE noteId = :noteId")
    suspend fun deleteAllVersionsForNote(noteId: Long)
}
