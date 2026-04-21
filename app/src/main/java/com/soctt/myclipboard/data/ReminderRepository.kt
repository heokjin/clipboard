package com.soctt.myclipboard.data

import android.content.Context
import com.soctt.myclipboard.data.local.ClipboardDatabase
import com.soctt.myclipboard.data.local.ReminderEntity
import com.soctt.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.flow.Flow

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
        appContext?.let { MyClipboardWidgetSync.refreshAll(it) }
    }

    suspend fun updateReminder(id: Long, text: String) {
        database.reminderDao().update(
            ReminderEntity(
                id = id,
                text = text,
                updatedAt = System.currentTimeMillis(),
            )
        )
        appContext?.let { MyClipboardWidgetSync.refreshAll(it) }
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        database.reminderDao().delete(reminder)
        appContext?.let { MyClipboardWidgetSync.refreshAll(it) }
    }
}
