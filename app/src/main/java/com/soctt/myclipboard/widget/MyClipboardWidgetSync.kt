package com.soctt.myclipboard.widget

import android.content.Context
import android.util.Log
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
            Log.d(WidgetDebugTag, "MyClipboardWidgetSync.refreshIfDirty -> updateAll start")
            MyClipboardAppWidget().updateAll(context.applicationContext)
            Log.d(WidgetDebugTag, "MyClipboardWidgetSync.refreshIfDirty -> updateAll requested")
        }
    }

    suspend fun refreshAll(context: Context) {
        refreshMutex.withLock {
            Log.d(WidgetDebugTag, "MyClipboardWidgetSync.refreshAll -> updateAll start")
            MyClipboardAppWidget().updateAll(context.applicationContext)
            Log.d(WidgetDebugTag, "MyClipboardWidgetSync.refreshAll -> updateAll requested")
        }
    }
}
