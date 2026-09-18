package com.notepad.app

import com.notepad.app.ui.common.parseInlineMarkdown
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testPlainTextParsing() {
        val parsed = parseInlineMarkdown("Plain sample text")
        assertEquals("Plain sample text", parsed.text)
    }

    @Test
    fun testBoldFormattingExtraction() {
        val parsed = parseInlineMarkdown("This is **important** note")
        assertEquals("This is important note", parsed.text)
    }

    @Test
    fun testInlineCodeFormattingExtraction() {
        val parsed = parseInlineMarkdown("Run `gradle build` command")
        assertEquals("Run  gradle build  command", parsed.text)
    }
}
