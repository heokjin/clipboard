package com.scott.myclipboard.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val WidgetDebugTag = "WidgetDebug"

object MyClipboardWidgetSync {
    private val isDirty = AtomicBoolean(false)
    private val refreshMutex = Mutex()

    fun markDirty() {
        isDirty.set(true)
        Log.d(WidgetDebugTag, "MyClipboardWidgetSync.markDirty dirty=true")
    }

    fun clearDirty(reason: String) {
        isDirty.set(false)
        Log.d(WidgetDebugTag, "MyClipboardWidgetSync.clearDirty reason=$reason dirty=false")
    }

    suspend fun refreshIfDirty(context: Context) {
        if (!isDirty.get()) {
            Log.d(WidgetDebugTag, "MyClipboardWidgetSync.refreshIfDirty skipped dirty=false")
            return
        }

        refreshMutex.withLock {
            if (!isDirty.get()) {
                Log.d(WidgetDebugTag, "MyClipboardWidgetSync.refreshIfDirty skipped after lock dirty=false")
                return
            }
            refreshWidgets(context.applicationContext, "refreshIfDirty")
        }
    }

    suspend fun refreshAll(context: Context) {
        refreshMutex.withLock {
            refreshWidgets(context.applicationContext, "refreshAll")
        }
    }

    private suspend fun refreshWidgets(
        context: Context,
        source: String,
    ) {
        val widget = MyClipboardAppWidget()
        val glanceIds = GlanceAppWidgetManager(context)
            .getGlanceIds(MyClipboardAppWidget::class.java)
        Log.d(WidgetDebugTag, "MyClipboardWidgetSync.$source -> direct update start ids=${glanceIds.size}")
        glanceIds.forEach { glanceId ->
            Log.d(WidgetDebugTag, "MyClipboardWidgetSync.$source -> update glanceId=$glanceId")
            widget.update(context, glanceId)
        }
        widget.updateAll(context)
        Log.d(WidgetDebugTag, "MyClipboardWidgetSync.$source -> updates requested ids=${glanceIds.size}")
    }
}
