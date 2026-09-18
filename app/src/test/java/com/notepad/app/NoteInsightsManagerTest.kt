package com.notepad.app

import com.notepad.app.core.insights.NoteInsightsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoteInsightsManagerTest {

    private lateinit var insightsManager: NoteInsightsManager

    @Before
    fun setUp() {
        insightsManager = NoteInsightsManager()
    }

    @Test
    fun testActionItemExtraction() {
        val content = """
            Meeting Notes:
            TODO: Prepare quarterly finance report
            We discussed several improvements.
            Follow up with design team on mockups
            Regular line here without actions.
            - [ ] Existing task from yesterday
        """.trimIndent()

        val tasks = insightsManager.extractTasks(content)

        assertEquals(3, tasks.size)
        assertTrue(tasks.any { it.contains("Prepare quarterly finance report") })
        assertTrue(tasks.any { it.contains("Follow up with design team on mockups") })
        assertTrue(tasks.any { it.contains("Existing task from yesterday") })
    }

    @Test
    fun testAppendTasksAsChecklist() {
        val original = "Project Overview"
        val tasks = listOf("Call client", "Send contract")

        val result = insightsManager.appendExtractedTasksAsChecklist(original, tasks)

        assertTrue(result.contains("### Extracted Tasks"))
        assertTrue(result.contains("- [ ] Call client"))
        assertTrue(result.contains("- [ ] Send contract"))
    }

    @Test
    fun testReadingTimeCalculation() {
        val shortContent = "Short note with five words."
        val shortInsights = insightsManager.analyze(shortContent)
        assertEquals(1, shortInsights.readingTimeMinutes)

        // 450 words -> should be 3 minutes (at 200 wpm)
        val longContent = (1..450).joinToString(" ") { "word$it" }
        val longInsights = insightsManager.analyze(longContent)
        assertEquals(3, longInsights.readingTimeMinutes)
    }
}
