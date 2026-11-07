package com.notepad.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.notepad.app.MainActivity
import com.notepad.app.R
import com.notepad.app.core.reminder.ReminderReceiver
import com.notepad.app.domain.repository.NoteRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun noteRepository(): NoteRepository
}

class StickyNoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java
                )
                val latestNote = entryPoint.noteRepository().getActiveNotes().firstOrNull()?.firstOrNull()

                for (appWidgetId in appWidgetIds) {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        if (latestNote != null) {
                            putExtra(ReminderReceiver.EXTRA_NOTE_ID, latestNote.id)
                        }
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val views = RemoteViews(context.packageName, R.layout.widget_sticky_note).apply {
                        if (latestNote != null) {
                            setTextViewText(R.id.widget_title, latestNote.title.ifBlank { "Untitled Note" })
                            setTextViewText(R.id.widget_content, latestNote.content.ifBlank { "Empty note" })
                        } else {
                            setTextViewText(R.id.widget_title, "Sticky Note")
                            setTextViewText(R.id.widget_content, "Tap to write a note...")
                        }
                        setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun notifyNoteChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, StickyNoteWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, StickyNoteWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
