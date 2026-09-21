package com.notepad.app.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AttachmentManager private constructor(
    private val context: Context?,
    @Suppress("UNUSED_PARAMETER") marker: Unit?
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, null)

    constructor() : this(null, null)

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null

    private fun getAttachmentsDir(): File? {
        val dir = File(context?.filesDir, "attachments")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    open suspend fun saveImageFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext null
        try {
            val dir = getAttachmentsDir() ?: return@withContext null
            val destFile = File(dir, "img_${System.currentTimeMillis()}.jpg")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    open suspend fun saveSketchBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val dir = getAttachmentsDir() ?: return@withContext null
            val destFile = File(dir, "sketch_${System.currentTimeMillis()}.png")
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    open fun startVoiceRecording(): String? {
        val ctx = context ?: return null
        return try {
            val dir = getAttachmentsDir() ?: return null
            val audioFile = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = audioFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            audioFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    open fun stopVoiceRecording(): String? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            currentRecordingFile?.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    open fun playAudio(filePath: String, onCompletion: () -> Unit = {}) {
        stopAudio()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    onCompletion()
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            onCompletion()
        }
    }

    open fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }
}
