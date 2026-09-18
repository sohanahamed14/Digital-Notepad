package com.notepad.app

import com.notepad.app.domain.model.Note
import com.notepad.app.domain.model.NoteCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteModelTest {

    @Test
    fun testWordCountCalculation() {
        val emptyNote = Note(title = "Test", content = "")
        assertEquals(0, emptyNote.wordCount)

        val singleWord = Note(title = "Test", content = "Hello")
        assertEquals(1, singleWord.wordCount)

        val multipleWords = Note(title = "Test", content = "Hello world from Android Notepad")
        assertEquals(5, multipleWords.wordCount)

        val withMultipleSpacesAndNewlines = Note(
            title = "Test",
            content = "  Line one \n\n  Line two with spaces   "
        )
        assertEquals(6, withMultipleSpacesAndNewlines.wordCount)
    }

    @Test
    fun testCharacterCount() {
        val note = Note(title = "Test", content = "Hello 123!")
        assertEquals(10, note.characterCount)
    }

    @Test
    fun testDefaultValues() {
        val note = Note()
        assertEquals(0L, note.id)
        assertEquals(NoteCategory.GENERAL.name, note.category)
        assertEquals(false, note.isPinned)
        assertEquals(false, note.isVaultLocked)
        assertEquals(false, note.isDeleted)
    }
}
