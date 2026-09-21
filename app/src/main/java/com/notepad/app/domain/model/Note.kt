package com.notepad.app.domain.model

data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val colorHex: Long = 0L,
    val category: String = NoteCategory.GENERAL.name,
    val isPinned: Boolean = false,
    val isVaultLocked: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val reminderAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val wordCount: Int
        get() = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size

    val characterCount: Int
        get() = content.length

    val totalChecklistItems: Int
        get() = content.lines().count {
            val t = it.trim()
            t.startsWith("- [ ]") || t.startsWith("- [x]") || t.startsWith("- [X]") ||
            t.startsWith("* [ ]") || t.startsWith("* [x]") || t.startsWith("* [X]")
        }

    val completedChecklistItems: Int
        get() = content.lines().count {
            val t = it.trim()
            t.startsWith("- [x]") || t.startsWith("- [X]") ||
            t.startsWith("* [x]") || t.startsWith("* [X]")
        }

    val checklistProgress: Float
        get() = if (totalChecklistItems == 0) 0f else completedChecklistItems.toFloat() / totalChecklistItems.toFloat()

    val tags: List<String>
        get() {
            val regex = Regex("""(?<=^|\s)#([a-zA-Z0-9_\-]+)""")
            return regex.findAll("$title $content")
                .map { it.groupValues[1].lowercase() }
                .distinct()
                .toList()
        }
}
