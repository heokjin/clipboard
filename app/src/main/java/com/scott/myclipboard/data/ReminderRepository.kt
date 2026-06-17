package com.scott.myclipboard.data

import android.content.Context
import android.util.Log
import com.scott.myclipboard.data.local.ClipboardDatabase
import com.scott.myclipboard.data.local.ReminderEntity
import com.scott.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

private const val WidgetDebugTag = "WidgetDebug"
private const val ReminderBackupPrefsName = "reminder_backup"
private const val ReminderBackupJsonKey = "reminder_backup_json"
private const val ReminderBackupSavedAtKey = "reminder_backup_saved_at"
private const val ReminderBackupKind = "reminder"

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
        val savedAtMillis = System.currentTimeMillis()
        context.getSharedPreferences(ReminderBackupPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(ReminderBackupJsonKey, reminders.toBackupJson(savedAtMillis))
            .putLong(ReminderBackupSavedAtKey, savedAtMillis)
            .apply()
        return reminders.size
    }

    fun getBackupExportJson(): String? {
        val context = appContext ?: return null
        return context.getSharedPreferences(ReminderBackupPrefsName, Context.MODE_PRIVATE)
            .getString(ReminderBackupJsonKey, null)
    }

    suspend fun importBackupJson(backupJson: String): Int? {
        val context = appContext ?: return null
        val reminders = backupJson.toBackupReminders() ?: return null
        val savedAtMillis = backupJson.toBackupSavedAtMillis()
            ?.takeIf { it > 0L }
            ?: System.currentTimeMillis()
        context.getSharedPreferences(ReminderBackupPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(ReminderBackupJsonKey, reminders.toBackupJson(savedAtMillis))
            .putLong(ReminderBackupSavedAtKey, savedAtMillis)
            .apply()
        val existingKeys = database.reminderDao().getAllReminders()
            .map { it.backupIdentityKey() }
            .toMutableSet()
        val now = System.currentTimeMillis()
        reminders.forEachIndexed { index, reminder ->
            if (existingKeys.add(reminder.backupIdentityKey())) {
                database.reminderDao().insert(
                    reminder.copy(
                        id = 0,
                        updatedAt = now + (reminders.size - index),
                    )
                )
            }
        }
        Log.d(WidgetDebugTag, "ReminderRepository.importBackupJson count=${reminders.size} -> markDirty")
        MyClipboardWidgetSync.markDirty()
        MyClipboardWidgetSync.refreshAll(context)
        return reminders.size
    }

    fun hasBackup(): Boolean {
        val context = appContext ?: return false
        return context.getSharedPreferences(ReminderBackupPrefsName, Context.MODE_PRIVATE)
            .contains(ReminderBackupJsonKey)
    }

    fun getBackupSavedAtMillis(): Long? {
        val context = appContext ?: return null
        val prefs = context.getSharedPreferences(ReminderBackupPrefsName, Context.MODE_PRIVATE)
        if (!prefs.contains(ReminderBackupJsonKey)) {
            return null
        }
        val savedAtMillis = prefs.getLong(ReminderBackupSavedAtKey, 0L)
        if (savedAtMillis > 0L) {
            return savedAtMillis
        }
        return prefs.getString(ReminderBackupJsonKey, null)
            ?.toBackupSavedAtMillis()
            ?.takeIf { it > 0L }
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

    private fun List<ReminderEntity>.toBackupJson(savedAtMillis: Long): String {
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
            .put("kind", ReminderBackupKind)
            .put("version", 1)
            .put("savedAtMillis", savedAtMillis)
            .put("items", items)
            .toString()
    }

    private fun ReminderEntity.backupIdentityKey(): String {
        return listOf(text, styleSpansJson, isImportant.toString()).joinToString(separator = "\u0000")
    }

    private fun String.toBackupSavedAtMillis(): Long? {
        return runCatching {
            JSONObject(this).optLong("savedAtMillis", 0L)
        }.getOrNull()
    }

    private fun String.toBackupReminders(): List<ReminderEntity>? {
        return try {
            val backup = JSONObject(this)
            val kind = backup.optString("kind", ReminderBackupKind)
            if (kind != ReminderBackupKind) {
                return null
            }
            val items = backup.optJSONArray("items") ?: return null
            buildList<ReminderEntity> {
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
        } catch (_: Exception) {
            null
        }
    }
}
