package com.notepad.app.core.template

import com.notepad.app.domain.model.NoteCategory

enum class NoteTemplate(
    val displayName: String,
    val description: String,
    val icon: String,
    val category: NoteCategory,
    val templateTitle: String,
    val templateContent: String
) {
    BLANK(
        displayName = "Blank Note",
        description = "Start from scratch",
        icon = "📝",
        category = NoteCategory.GENERAL,
        templateTitle = "",
        templateContent = ""
    ),
    MEETING_NOTES(
        displayName = "Meeting Notes",
        description = "Agenda, attendees & action items",
        icon = "🤝",
        category = NoteCategory.WORK,
        templateTitle = "Meeting Notes",
        templateContent = """# Meeting Notes

**Date:** ${"\${DATE}"}
**Attendees:**
- [ ] @person1
- [ ] @person2

---

## Agenda
1. 
2. 
3. 

## Discussion Notes


## Action Items
- [ ] Action item 1 — Owner: 
- [ ] Action item 2 — Owner: 
- [ ] Action item 3 — Owner: 

## Decisions Made


## Next Meeting

"""
    ),
    JOURNAL_ENTRY(
        displayName = "Journal Entry",
        description = "Daily reflection & gratitude",
        icon = "📔",
        category = NoteCategory.PERSONAL,
        templateTitle = "Journal Entry",
        templateContent = """# Journal Entry

**Date:** ${"\${DATE}"}

## 🌅 Morning Intention


## 🙏 Gratitude
1. 
2. 
3. 

## 📝 Today's Highlights


## 💭 Reflections


## 🎯 Tomorrow's Focus

"""
    ),
    CORNELL_NOTES(
        displayName = "Cornell Notes",
        description = "Structured study notes",
        icon = "🎓",
        category = NoteCategory.IDEAS,
        templateTitle = "Cornell Notes",
        templateContent = """# Cornell Notes — Topic

## Cue / Questions
> Key questions or cues go here to trigger recall

## Notes
Main notes and details go here. Focus on:
- Key concepts
- Important details
- Examples and illustrations

## Summary
*Write a brief summary of the notes in your own words:*


"""
    ),
    BUG_REPORT(
        displayName = "Bug Report",
        description = "Steps to reproduce & environment",
        icon = "🐛",
        category = NoteCategory.WORK,
        templateTitle = "Bug Report",
        templateContent = """# Bug Report

**Severity:** 🔴 High / 🟡 Medium / 🟢 Low
**Status:** Open
**Date:** ${"\${DATE}"}

## Description


## Steps to Reproduce
1. 
2. 
3. 

## Expected Behavior


## Actual Behavior


## Environment
- **Device:** 
- **OS:** 
- **App Version:** 

## Screenshots / Logs


## Possible Fix

"""
    ),
    WEEKLY_REVIEW(
        displayName = "Weekly Review",
        description = "Wins, challenges & next goals",
        icon = "📊",
        category = NoteCategory.TODO,
        templateTitle = "Weekly Review",
        templateContent = """# Weekly Review

**Week of:** ${"\${DATE}"}

## 🏆 Wins This Week
- [ ] 
- [ ] 
- [ ] 

## 🚧 Challenges
- 
- 

## 📊 Key Metrics / Progress


## 📋 Carry-Over Tasks
- [ ] 
- [ ] 

## 🎯 Next Week Goals
- [ ] Goal 1
- [ ] Goal 2
- [ ] Goal 3

## 💡 Ideas & Notes

"""
    );

    companion object {
        /** Replace date placeholder with today's date */
        fun resolveContent(template: NoteTemplate): String {
            val today = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault())
                .format(java.util.Date())
            return template.templateContent.replace("\${DATE}", today)
        }
    }
}
