package com.notepad.app.ui.common

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onCheckboxToggled: ((lineIndex: Int, newChecked: Boolean) -> Unit)? = null
) {
    val lines = markdown.lines()
    var inCodeBlock = false
    val codeBlockBuilder = StringBuilder()

    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()

            // Handle Fenced Code Blocks
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    CodeBlock(code = codeBlockBuilder.toString().trimEnd())
                    codeBlockBuilder.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                return@forEachIndexed
            }

            if (inCodeBlock) {
                codeBlockBuilder.append(line).append("\n")
                return@forEachIndexed
            }

            // Headers
            when {
                trimmed.startsWith("# ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("## ")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseInlineMarkdown(trimmed.removePrefix("### ")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }

                // Checklists
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                    val label = trimmed.substring(6)
                    ChecklistItem(
                        checked = false,
                        text = parseInlineMarkdown(label),
                        textColor = textColor,
                        onCheckedChange = { checked ->
                            onCheckboxToggled?.invoke(index, checked)
                        }
                    )
                }
                trimmed.startsWith("- [x] ") || trimmed.startsWith("* [x] ") ||
                trimmed.startsWith("- [X] ") || trimmed.startsWith("* [X] ") -> {
                    val label = trimmed.substring(6)
                    ChecklistItem(
                        checked = true,
                        text = parseInlineMarkdown(label),
                        textColor = textColor,
                        onCheckedChange = { checked ->
                            onCheckboxToggled?.invoke(index, checked)
                        }
                    )
                }

                // Blockquotes
                trimmed.startsWith("> ") -> {
                    Blockquote(text = parseInlineMarkdown(trimmed.removePrefix("> ")))
                }

                // Unordered List
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                        Text(text = "• ", style = MaterialTheme.typography.bodyLarge, color = textColor)
                        Text(
                            text = parseInlineMarkdown(trimmed.substring(2)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }
                }

                // Empty line
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Image Attachment: ![alt](path)
                trimmed.startsWith("![") && trimmed.contains("](") && trimmed.endsWith(")") -> {
                    val path = trimmed.substringAfter("](").substringBeforeLast(")")
                    val cleanPath = path.removePrefix("file://")
                    val file = File(cleanPath)
                    if (file.exists()) {
                        val bitmap = remember(cleanPath) {
                            try { android.graphics.BitmapFactory.decodeFile(cleanPath) } catch (_: Exception) { null }
                        }
                        if (bitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Attachment",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        } else {
                            Text(text = "🖼 [Image: ${file.name}]", color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Text(text = "🖼 [Image: $path]", color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Voice Note: 🎙 [Voice Memo: path]
                trimmed.startsWith("🎙 [Voice Memo:") && trimmed.endsWith("]") -> {
                    val audioPath = trimmed.substringAfter("🎙 [Voice Memo:").substringBeforeLast("]").trim()
                    AudioPlayerItem(filePath = audioPath)
                }

                // Plain Body Text with inline formatting
                else -> {
                    Text(
                        text = parseInlineMarkdown(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // Catch unclosed code block
        if (inCodeBlock && codeBlockBuilder.isNotEmpty()) {
            CodeBlock(code = codeBlockBuilder.toString().trimEnd())
        }
    }
}

@Composable
private fun ChecklistItem(
    checked: Boolean,
    text: AnnotatedString,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (checked) MaterialTheme.colorScheme.outline else textColor
        )
    }
}

@Composable
private fun Blockquote(text: AnnotatedString) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .padding(12.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = Color(0xFF9CDCFE)
        )
    }
}

fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Bold: **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Inline Code: `code`
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x33888888),
                                fontSize = 13.sp
                            )
                        )
                        append(" ${text.substring(i + 1, end)} ")
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic: *text*
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

@Composable
private fun AudioPlayerItem(filePath: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(filePath) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        isPlaying = false
                    } else {
                        try {
                            val player = MediaPlayer().apply {
                                setDataSource(filePath)
                                setOnCompletionListener {
                                    isPlaying = false
                                    it.release()
                                    mediaPlayer = null
                                }
                                prepare()
                                start()
                            }
                            mediaPlayer = player
                            isPlaying = true
                        } catch (_: Exception) {
                            isPlaying = false
                        }
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎙 Voice Memo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isPlaying) "Playing audio..." else "Tap to play recording",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

