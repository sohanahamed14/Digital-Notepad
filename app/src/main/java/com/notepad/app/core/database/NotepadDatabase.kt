package com.notepad.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.notepad.app.data.local.dao.NoteDao
import com.notepad.app.data.local.dao.NoteVersionDao
import com.notepad.app.data.local.entity.NoteEntity
import com.notepad.app.data.local.entity.NoteFtsEntity
import com.notepad.app.data.local.entity.NoteVersionEntity

@Database(
    entities = [
        NoteEntity::class,
        NoteFtsEntity::class,
        NoteVersionEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NotepadDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun noteVersionDao(): NoteVersionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Reset colorHex from background color to 0 (theme default text color)
                db.execSQL("UPDATE notes SET colorHex = 0")
            }
        }
    }
}
