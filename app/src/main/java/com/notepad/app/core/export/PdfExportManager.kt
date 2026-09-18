package com.notepad.app.core.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.notepad.app.domain.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 48f

    fun generatePdf(note: Note): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedTitle = note.title.trim().replace("[^a-zA-Z0-9.-]".toRegex(), "_").ifBlank { "Untitled" }
        val pdfFile = File(exportDir, "${sanitizedTitle}_${note.id}.pdf")

        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("MMMM d, yyyy • HH:mm", Locale.getDefault())

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = margin + 20f

        // Draw Header: Title
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(33, 33, 33)
        canvas.drawText(note.title.ifBlank { "Untitled Note" }, margin, y, paint)
        y += 26f

        // Metadata: Category & Date
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(117, 117, 117)
        val metaText = "Category: ${note.category}   |   Last Updated: ${dateFormat.format(Date(note.updatedAt))}"
        canvas.drawText(metaText, margin, y, paint)
        y += 16f

        // Accent Separator Line
        paint.strokeWidth = 2f
        paint.color = Color.rgb(103, 80, 164)
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        y += 24f

        // Content Paragraphs
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(40, 40, 40)
        paint.strokeWidth = 0f

        val maxLineWidth = pageWidth - (margin * 2)
        val lines = note.content.lines()

        for (rawLine in lines) {
            val words = rawLine.split(" ")
            var currentLine = ""

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val measuredWidth = paint.measureText(testLine)

                if (measuredWidth > maxLineWidth) {
                    canvas.drawText(currentLine, margin, y, paint)
                    y += 18f

                    // Check for page overflow
                    if (y > pageHeight - margin) {
                        document.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        y = margin + 20f
                    }
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }

            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, margin, y, paint)
                y += 18f
            } else {
                y += 10f // Empty line spacing
            }

            // Check for page overflow after line break
            if (y > pageHeight - margin) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin + 20f
            }
        }

        document.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    fun sharePdf(note: Note): Intent {
        val file = generatePdf(note)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    fun shareMarkdown(note: Note): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, "# ${note.title}\n\n${note.content}")
        }
    }
}
