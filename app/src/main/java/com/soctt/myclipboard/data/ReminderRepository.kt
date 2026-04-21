package com.soctt.myclipboard.data

import android.content.Context
import android.util.Log
import com.soctt.myclipboard.data.local.ClipboardDatabase
import com.soctt.myclipboard.data.local.ReminderEntity
import com.soctt.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.flow.Flow

private const val WidgetDebugTag = "WidgetDebug"

class ReminderRepository(
    private val database: ClipboardDatabase,
    private val appContext: Context? = null,
) {
    constructor(context: Context) : this(
        database = ClipboardDatabase.getDatabase(context),
        appContext = context.applicationContext,
    )

    fun observeReminders(): Flow<List<ReminderEntity>> {
        return database.reminderDao().observeReminders()
    }

    fun observeRecentReminders(limit: Int): Flow<List<ReminderEntity>> {
        return database.reminderDao().observeRecentReminders(limit)
    }

    suspend fun getRecentReminders(limit: Int): List<ReminderEntity> {
        return database.reminderDao().getRecentReminders(limit)
    }

    suspend fun getReminderById(id: Long): ReminderEntity? {
        return database.reminderDao().getReminderById(id)
    }

    suspend fun addReminder(text: String) {
        database.reminderDao().insert(
            ReminderEntity(text = text)
        )
        Log.d(WidgetDebugTag, "ReminderRepository.addReminder -> markDirty")
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            Log.d(WidgetDebugTag, "ReminderRepository.addReminder -> refreshAll immediate")
            MyClipboardWidgetSync.refreshAll(context)
        }
    }

    suspend fun updateReminder(id: Long, text: String) {
        database.reminderDao().update(
            ReminderEntity(
                id = id,
                text = text,
                updatedAt = System.currentTimeMillis(),
            )
        )
        Log.d(WidgetDebugTag, "ReminderRepository.updateReminder id=$id -> markDirty")
        MyClipboardWidgetSync.markDirty()
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        database.reminderDao().delete(reminder)
        Log.d(WidgetDebugTag, "ReminderRepository.deleteReminder id=${reminder.id} -> markDirty")
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            Log.d(WidgetDebugTag, "ReminderRepository.deleteReminder id=${reminder.id} -> refreshAll immediate")
            MyClipboardWidgetSync.refreshAll(context)
        }
    }
}
