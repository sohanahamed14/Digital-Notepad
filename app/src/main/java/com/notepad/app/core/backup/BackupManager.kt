package com.notepad.app.core.backup

import android.content.Context
import com.notepad.app.domain.model.Note
import com.notepad.app.domain.repository.NoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val errorMessage: String? = null
)

@Singleton
open class BackupManager private constructor(
    private val context: Context?,
    @Suppress("UNUSED_PARAMETER") marker: Unit?
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, null)

    constructor() : this(null, null)

    private val secureRandom = SecureRandom()

    open suspend fun exportNotesToJson(repository: NoteRepository, outputStream: OutputStream): Int = withContext(Dispatchers.IO) {
        val notes = repository.getActiveNotes().first()
        val root = buildJsonForNotes(notes)
        outputStream.bufferedWriter().use { writer ->
            writer.write(root.toString(2))
        }
        notes.size
    }

    open suspend fun importNotesFromJson(repository: NoteRepository, inputStream: InputStream): ImportResult = withContext(Dispatchers.IO) {
        try {
            val jsonText = inputStream.bufferedReader().use { it.readText() }
            parseAndSaveJsonNotes(repository, jsonText)
        } catch (e: Exception) {
            ImportResult(0, 1, e.message ?: "Failed to import notes")
        }
    }

    open suspend fun exportEncryptedBackup(repository: NoteRepository, outputStream: OutputStream, passphrase: String): Int = withContext(Dispatchers.IO) {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }
        val activeNotes = repository.getActiveNotes().first()
        val vaultNotes = repository.getVaultNotes().first()
        val allNotes = activeNotes + vaultNotes
        val root = buildJsonForNotes(allNotes)
        val plainBytes = root.toString().toByteArray(Charsets.UTF_8)

        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }

        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plainBytes)

        outputStream.use { out ->
            out.write(HEADER_MAGIC)
            out.write(salt)
            out.write(iv)
            out.write(ciphertext)
            out.flush()
        }
        allNotes.size
    }

    open suspend fun importEncryptedBackup(repository: NoteRepository, inputStream: InputStream, passphrase: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }
            val rawBytes = inputStream.use { it.readBytes() }
            val magicLen = HEADER_MAGIC.size
            if (rawBytes.size < magicLen + 16 + 12) {
                return@withContext ImportResult(0, 0, "Corrupted or invalid encrypted backup file")
            }

            for (i in HEADER_MAGIC.indices) {
                if (rawBytes[i] != HEADER_MAGIC[i]) {
                    return@withContext ImportResult(0, 0, "Invalid file format: not an encrypted notepad backup")
                }
            }

            var offset = magicLen
            val salt = rawBytes.copyOfRange(offset, offset + 16)
            offset += 16
            val iv = rawBytes.copyOfRange(offset, offset + 12)
            offset += 12
            val ciphertext = rawBytes.copyOfRange(offset, rawBytes.size)

            val key = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val plainBytes = cipher.doFinal(ciphertext)
            val jsonText = String(plainBytes, Charsets.UTF_8)

            parseAndSaveJsonNotes(repository, jsonText)
        } catch (e: javax.crypto.AEADBadTagException) {
            ImportResult(0, 0, "Incorrect passphrase or corrupted encrypted backup")
        } catch (e: Exception) {
            ImportResult(0, 1, e.message ?: "Failed to restore encrypted backup")
        }
    }

    open suspend fun exportNotesToZip(repository: NoteRepository, outputStream: OutputStream): Int = withContext(Dispatchers.IO) {
        val notes = repository.getActiveNotes().first()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        ZipOutputStream(outputStream.buffered()).use { zipOut ->
            for ((index, note) in notes.withIndex()) {
                val safeTitle = note.title.ifBlank { "Untitled" }
                    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    .take(40)
                val fileName = "${index + 1}_$safeTitle.md"
                val entry = ZipEntry(fileName)
                zipOut.putNextEntry(entry)

                val content = buildString {
                    appendLine("# ${note.title.ifBlank { "Untitled Note" }}")
                    appendLine()
                    appendLine("> Category: ${note.category} | Updated: ${dateFormat.format(Date(note.updatedAt))}")
                    if (note.tags.isNotEmpty()) {
                        appendLine("> Tags: ${note.tags.joinToString(" ")}")
                    }
                    appendLine()
                    appendLine(note.content)
                }
                zipOut.write(content.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }
        }
        notes.size
    }

    open suspend fun importFromTextOrKeep(repository: NoteRepository, inputStream: InputStream, fileName: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val text = inputStream.bufferedReader().use { it.readText() }
            if (fileName.endsWith(".json", ignoreCase = true)) {
                if (text.trimStart().startsWith("{")) {
                    val root = JSONObject(text)
                    if (root.has("notes")) {
                        return@withContext parseAndSaveJsonNotes(repository, text)
                    } else if (root.has("textContent") || root.has("text") || root.has("title")) {
                        // Google Keep individual JSON note
                        val title = root.optString("title", "").ifBlank { "Imported Keep Note" }
                        val content = root.optString("textContent", root.optString("text", ""))
                        val note = Note(
                            id = 0L,
                            title = title,
                            content = content,
                            category = "KEEP_IMPORT",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.saveNote(note, createSnapshot = false)
                        return@withContext ImportResult(successCount = 1)
                    }
                }
            }

            // Plain text or Markdown file
            val lines = text.lines()
            val firstLine = lines.firstOrNull { it.isNotBlank() }?.trim()?.removePrefix("#")?.trim()
            val title = if (!firstLine.isNullOrBlank()) firstLine else fileName.substringBeforeLast(".")
            val content = if (lines.size > 1) lines.drop(1).joinToString("\n").trim() else text

            val note = Note(
                id = 0L,
                title = title,
                content = content,
                category = "IMPORTED",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveNote(note, createSnapshot = false)
            ImportResult(successCount = 1)
        } catch (e: Exception) {
            ImportResult(0, 1, e.message ?: "Failed to import file")
        }
    }

    private fun buildJsonForNotes(notes: List<Note>): JSONObject {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "Digital Notepad")

        val array = JSONArray()
        for (note in notes) {
            val noteObj = JSONObject()
            noteObj.put("title", note.title)
            noteObj.put("content", note.content)
            noteObj.put("category", note.category)
            noteObj.put("colorHex", note.colorHex)
            noteObj.put("isPinned", note.isPinned)
            noteObj.put("isVaultLocked", note.isVaultLocked)
            noteObj.put("createdAt", note.createdAt)
            noteObj.put("updatedAt", note.updatedAt)
            array.put(noteObj)
        }
        root.put("notes", array)
        return root
    }

    private suspend fun parseAndSaveJsonNotes(repository: NoteRepository, jsonText: String): ImportResult {
        val root = JSONObject(jsonText)
        val array = root.optJSONArray("notes") ?: return ImportResult(0, 0, "Invalid backup format: 'notes' array missing")

        var success = 0
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val note = Note(
                id = 0L,
                title = obj.optString("title", ""),
                content = obj.optString("content", ""),
                category = obj.optString("category", "GENERAL"),
                colorHex = obj.optLong("colorHex", 0L),
                isPinned = obj.optBoolean("isPinned", false),
                isVaultLocked = obj.optBoolean("isVaultLocked", false),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
            repository.saveNote(note, createSnapshot = false)
            success++
        }
        return ImportResult(successCount = success)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    companion object {
        private val HEADER_MAGIC = "E2EE_BAK1".toByteArray(Charsets.UTF_8)
        private const val ITERATIONS = 10_000
        private const val KEY_LENGTH_BITS = 256
    }
}
