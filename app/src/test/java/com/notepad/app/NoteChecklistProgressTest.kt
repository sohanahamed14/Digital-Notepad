package com.notepad.app

import com.notepad.app.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteChecklistProgressTest {

    @Test
    fun testNoChecklistsReturnsZero() {
        val note = Note(content = "Just plain text without tasks.")
        assertEquals(0, note.totalChecklistItems)
        assertEquals(0, note.completedChecklistItems)
        assertEquals(0f, note.checklistProgress, 0.001f)
    }

    @Test
    fun testPartialChecklistProgress() {
        val note = Note(
            content = """
                - [x] Task 1 completed
                - [ ] Task 2 pending
                - [X] Task 3 completed uppercase
                - [ ] Task 4 pending
            """.trimIndent()
        )

        assertEquals(4, note.totalChecklistItems)
        assertEquals(2, note.completedChecklistItems)
        assertEquals(0.5f, note.checklistProgress, 0.001f)
    }

    @Test
    fun testFullyCompletedChecklist() {
        val note = Note(
            content = """
                * [x] All done 1
                - [x] All done 2
            """.trimIndent()
        )

        assertEquals(2, note.totalChecklistItems)
        assertEquals(2, note.completedChecklistItems)
        assertEquals(1.0f, note.checklistProgress, 0.001f)
    }
}
