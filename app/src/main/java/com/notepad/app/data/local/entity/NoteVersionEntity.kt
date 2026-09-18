package com.notepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_versions",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class NoteVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val versionId: Long = 0,
    val noteId: Long,
    val title: String,
    val content: String,
    val savedAt: Long = System.currentTimeMillis()
)
