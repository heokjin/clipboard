package com.soctt.myclipboard.data

import android.content.Context
import android.util.Log
import com.soctt.myclipboard.data.local.ClipboardDatabase
import com.soctt.myclipboard.data.local.ReminderEntity
import com.soctt.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

private const val WidgetDebugTag = "WidgetDebug"
private const val ReminderBackupPrefsName = "reminder_backup"
private const val ReminderBackupJsonKey = "reminder_backup_json"

class ReminderRepository(
    private val database: ClipboardDatabase,
    private val appContext: Context? = null,
) {
    constructor(context: Context) : this(
        database = ClipboardDatabase.getDatabase(context),
        appContext = context.applicationContext,
    )

    fun observeReminders(
        query: String,
        pinImportantToTop: Boolean,
    ): Flow<List<ReminderEntity>> {
        return database.reminderDao().observeReminders(
            query = query.trim(),
            pinImportantToTop = pinImportantToTop,
        )
    }

    fun observeRecentReminders(limit: Int): Flow<List<ReminderEntity>> {
        return database.reminderDao().observeRecentReminders(limit)
    }

    suspend fun getRecentReminders(limit: Int): List<ReminderEntity> {
        return database.reminderDao().getRecentReminders(limit)
    }

    suspend fun getAllReminders(): List<ReminderEntity> {
        return database.reminderDao().getAllReminders()
    }

    suspend fun getReminderById(id: Long): ReminderEntity? {
        return database.reminderDao().getReminderById(id)
    }

    suspend fun addReminder(
        text: String,
        styleSpansJson: String = "[]",
    ) {
        database.reminderDao().insert(
            ReminderEntity(
                text = text,
                styleSpansJson = styleSpansJson,
            )
        )
        Log.d(WidgetDebugTag, "ReminderRepository.addReminder -> markDirty")
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            Log.d(WidgetDebugTag, "ReminderRepository.addReminder -> refreshAll immediate")
            MyClipboardWidgetSync.refreshAll(context)
        }
    }

    suspend fun updateReminder(
        id: Long,
        text: String,
        styleSpansJson: String = "[]",
    ) {
        val existingReminder = database.reminderDao().getReminderById(id) ?: return
        database.reminderDao().update(
            existingReminder.copy(
                text = text,
                styleSpansJson = styleSpansJson,
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

    suspend fun restoreReminder(reminder: ReminderEntity) {
        database.reminderDao().insert(
            reminder.copy(updatedAt = System.currentTimeMillis())
        )
        Log.d(WidgetDebugTag, "ReminderRepository.restoreReminder id=${reminder.id} -> markDirty")
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            Log.d(WidgetDebugTag, "ReminderRepository.restoreReminder id=${reminder.id} -> refreshAll immediate")
            MyClipboardWidgetSync.refreshAll(context)
        }
    }

    suspend fun setReminderImportant(id: Long, isImportant: Boolean) {
        val existingReminder = database.reminderDao().getReminderById(id) ?: return
        database.reminderDao().update(
            existingReminder.copy(
                isImportant = isImportant,
                updatedAt = System.currentTimeMillis(),
            )
        )
        Log.d(WidgetDebugTag, "ReminderRepository.setReminderImportant id=$id important=$isImportant -> markDirty")
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            MyClipboardWidgetSync.refreshAll(context)
        }
    }

    suspend fun deleteAllReminders() {
        database.reminderDao().deleteAll()
        Log.d(WidgetDebugTag, "ReminderRepository.deleteAllReminders -> markDirty")
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            MyClipboardWidgetSync.refreshAll(context)
        }
    }

    suspend fun saveCurrentBackup(): Int {
        val context = appContext ?: return 0
        val reminders = getAllReminders()
        context.getSharedPreferences(ReminderBackupPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(ReminderBackupJsonKey, reminders.toBackupJson())
            .apply()
        return reminders.size
    }

    fun hasBackup(): Boolean {
        val context = appContext ?: return false
        return context.getSharedPreferences(ReminderBackupPrefsName, Context.MODE_PRIVATE)
            .contains(ReminderBackupJsonKey)
    }

    suspend fun restoreBackup(): Int? {
        val context = appContext ?: return null
        val backupJson = context.getSharedPreferences(ReminderBackupPrefsName, Context.MODE_PRIVATE)
            .getString(ReminderBackupJsonKey, null)
            ?: return null
        val reminders = backupJson.toBackupReminders() ?: return null

        val now = System.currentTimeMillis()
        database.reminderDao().deleteAll()
        reminders.forEachIndexed { index, reminder ->
            database.reminderDao().insert(
                reminder.copy(
                    id = 0,
                    updatedAt = now + (reminders.size - index),
                )
            )
        }
        Log.d(WidgetDebugTag, "ReminderRepository.restoreBackup count=${reminders.size} -> markDirty")
        MyClipboardWidgetSync.markDirty()
        MyClipboardWidgetSync.refreshAll(context)
        return reminders.size
    }

    private fun List<ReminderEntity>.toBackupJson(): String {
        val items = JSONArray()
        forEach { reminder ->
            items.put(
                JSONObject()
                    .put("text", reminder.text)
                    .put("styleSpansJson", reminder.styleSpansJson)
                    .put("isImportant", reminder.isImportant)
            )
        }
        return JSONObject()
            .put("version", 1)
            .put("items", items)
            .toString()
    }

    private fun String.toBackupReminders(): List<ReminderEntity>? {
        return runCatching {
            val items = JSONObject(this).optJSONArray("items") ?: JSONArray()
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val text = item.optString("text").trim()
                    if (text.isBlank()) {
                        continue
                    }
                    add(
                        ReminderEntity(
                            text = text,
                            styleSpansJson = item.optString("styleSpansJson", "[]"),
                            isImportant = item.optBoolean("isImportant", false),
                        )
                    )
                }
            }
        }.getOrNull()
    }
}
