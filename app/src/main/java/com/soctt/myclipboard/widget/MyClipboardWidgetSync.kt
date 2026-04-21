package com.soctt.myclipboard.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object MyClipboardWidgetSync {
    suspend fun refreshAll(context: Context) {
        MyClipboardAppWidget().updateAll(context)
    }
}
