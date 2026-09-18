package com.notepad.app.di

import android.content.Context
import androidx.room.Room
import com.notepad.app.core.database.NotepadDatabase
import com.notepad.app.data.local.dao.NoteDao
import com.notepad.app.data.local.dao.NoteVersionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNotepadDatabase(
        @ApplicationContext context: Context
    ): NotepadDatabase {
        return Room.databaseBuilder(
            context,
            NotepadDatabase::class.java,
            "notepad_database.db"
        )
            .addMigrations(NotepadDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideNoteDao(database: NotepadDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    fun provideNoteVersionDao(database: NotepadDatabase): NoteVersionDao {
        return database.noteVersionDao()
    }
}
