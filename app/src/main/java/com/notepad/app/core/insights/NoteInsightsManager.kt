package com.notepad.app.core.insights

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

data class NoteInsights(
    val readingTimeMinutes: Int,
    val wordCount: Int,
    val characterCount: Int,
    val extractedTasks: List<String>,
    val keyTakeaways: List<String>
)

@Singleton
class NoteInsightsManager @Inject constructor() {

    private val actionKeywords = listOf(
        "todo", "must", "need to", "action", "deadline", "follow up",
        "remind", "fix", "send", "call", "schedule", "buy", "review"
    )

    fun analyze(content: String): NoteInsights {
        val trimmed = content.trim()
        val words = if (trimmed.isBlank()) 0 else trimmed.split("\\s+".toRegex()).size
        val chars = trimmed.length
        val readingTime = if (words == 0) 0 else ceil(words / 200.0).toInt().coerceAtLeast(1)

        val extractedTasks = extractTasks(content)
        val takeaways = extractKeyTakeaways(content)

        return NoteInsights(
            readingTimeMinutes = readingTime,
            wordCount = words,
            characterCount = chars,
            extractedTasks = extractedTasks,
            keyTakeaways = takeaways
        )
    }

    fun extractTasks(content: String): List<String> {
        val tasks = mutableListOf<String>()
        val lines = content.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            // Already a checklist item
            if (trimmed.startsWith("- [ ]") || trimmed.startsWith("* [ ]")) {
                tasks.add(trimmed.substring(5).trim())
                continue
            }

            // Keyword triggers
            val lower = trimmed.lowercase()
            val matchesTrigger = actionKeywords.any { keyword ->
                lower.startsWith("$keyword:") ||
                lower.startsWith("$keyword ") ||
                lower.contains(" $keyword ")
            }

            if (matchesTrigger) {
                // Clean up markdown markers
                val cleaned = trimmed.removePrefix("- ").removePrefix("* ").trim()
                tasks.add(cleaned)
            }
        }

        return tasks.distinct()
    }

    private fun extractKeyTakeaways(content: String): List<String> {
        val sentences = content.split("(?<=[.!?])\\s+".toRegex())
            .map { it.trim().removePrefix("#").removePrefix("-").removePrefix("*").trim() }
            .filter { it.length in 20..180 && !it.startsWith("```") }

        return sentences.take(4)
    }

    fun appendExtractedTasksAsChecklist(originalContent: String, tasks: List<String>): String {
        if (tasks.isEmpty()) return originalContent
        val checklistSection = StringBuilder("\n\n### Extracted Tasks\n")
        tasks.forEach { task ->
            checklistSection.append("- [ ] $task\n")
        }
        return originalContent.trimEnd() + checklistSection.toString()
    }
}
