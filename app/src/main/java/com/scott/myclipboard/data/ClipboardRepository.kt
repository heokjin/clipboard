package com.scott.myclipboard.data

import android.content.Context
import com.scott.myclipboard.data.local.ClipboardDatabase
import com.scott.myclipboard.data.local.ClipboardPhraseEntity
import com.scott.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

private const val ClipboardBackupPrefsName = "clipboard_backup"
private const val ClipboardBackupJsonKey = "clipboard_backup_json"
private const val ClipboardBackupSavedAtKey = "clipboard_backup_saved_at"
private const val ClipboardBackupKind = "clipboard"

class ClipboardRepository(
    private val database: ClipboardDatabase,
    private val appContext: Context? = null,
) {
    constructor(context: Context) : this(
        database = ClipboardDatabase.getDatabase(context),
        appContext = context.applicationContext,
    )

    fun observePhrases(
        query: String,
        pinFavoritesToTop: Boolean,
    ): Flow<List<ClipboardPhraseEntity>> {
        return database.clipboardPhraseDao().observePhrases(
            query = query.trim(),
            pinFavoritesToTop = pinFavoritesToTop,
        )
    }

    fun observeRecentPhrases(limit: Int): Flow<List<ClipboardPhraseEntity>> {
        return database.clipboardPhraseDao().observeRecentPhrases(limit)
    }

    suspend fun getRecentPhrases(limit: Int): List<ClipboardPhraseEntity> {
        return database.clipboardPhraseDao().getRecentPhrases(limit)
    }

    suspend fun getAllPhrases(): List<ClipboardPhraseEntity> {
        return database.clipboardPhraseDao().getAllPhrases()
    }

    suspend fun getPhraseById(id: Long): ClipboardPhraseEntity? {
        return database.clipboardPhraseDao().getPhraseById(id)
    }

    suspend fun addPhrase(
        title: String,
        content: String,
        preventDuplicates: Boolean = false,
    ): Boolean {
        if (preventDuplicates && database.clipboardPhraseDao().getPhraseByContent(content) != null) {
            return false
        }

        database.clipboardPhraseDao().insert(
            ClipboardPhraseEntity(
                title = title,
                content = content,
            )
        )
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            MyClipboardWidgetSync.refreshAll(context)
        }
        return true
    }

    suspend fun updatePhrase(id: Long, title: String, content: String) {
        val existingPhrase = database.clipboardPhraseDao().getPhraseById(id) ?: return
        database.clipboardPhraseDao().update(
            existingPhrase.copy(
                title = title,
                content = content,
                updatedAt = System.currentTimeMillis(),
            )
        )
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            MyClipboardWidgetSync.refreshAll(context)
        }
    }

    suspend fun deletePhrase(phrase: ClipboardPhraseEntity) {
        database.clipboardPhraseDao().delete(phrase)
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            MyClipboardWidgetSync.refreshAll(context)
        }
    }

    suspend fun setPhraseFavorite(id: Long, isFavorite: Boolean) {
        val existingPhrase = database.clipboardPhraseDao().getPhraseById(id) ?: return
        database.clipboardPhraseDao().update(
            existingPhrase.copy(
                isFavorite = isFavorite,
                updatedAt = System.currentTimeMillis(),
            )
        )
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            MyClipboardWidgetSync.refreshAll(context)
        }
    }

    suspend fun deleteAllPhrases() {
        database.clipboardPhraseDao().deleteAll()
        MyClipboardWidgetSync.markDirty()
        appContext?.let { context ->
            MyClipboardWidgetSync.refreshAll(context)
        }
    }

    suspend fun saveCurrentBackup(): Int {
        val context = appContext ?: return 0
        val phrases = getAllPhrases()
        val savedAtMillis = System.currentTimeMillis()
        context.getSharedPreferences(ClipboardBackupPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(ClipboardBackupJsonKey, phrases.toBackupJson(savedAtMillis))
            .putLong(ClipboardBackupSavedAtKey, savedAtMillis)
            .apply()
        return phrases.size
    }

    fun getBackupExportJson(): String? {
        val context = appContext ?: return null
        return context.getSharedPreferences(ClipboardBackupPrefsName, Context.MODE_PRIVATE)
            .getString(ClipboardBackupJsonKey, null)
    }

    suspend fun importBackupJson(backupJson: String): Int? {
        val context = appContext ?: return null
        val phrases = backupJson.toBackupPhrases() ?: return null
        val savedAtMillis = backupJson.toBackupSavedAtMillis()
            ?.takeIf { it > 0L }
            ?: System.currentTimeMillis()
        context.getSharedPreferences(ClipboardBackupPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(ClipboardBackupJsonKey, phrases.toBackupJson(savedAtMillis))
            .putLong(ClipboardBackupSavedAtKey, savedAtMillis)
            .apply()
        val existingKeys = database.clipboardPhraseDao().getAllPhrases()
            .map { it.backupIdentityKey() }
            .toMutableSet()
        val now = System.currentTimeMillis()
        phrases.forEachIndexed { index, phrase ->
            if (existingKeys.add(phrase.backupIdentityKey())) {
                database.clipboardPhraseDao().insert(
                    phrase.copy(
                        id = 0,
                        updatedAt = now + (phrases.size - index),
                    )
                )
            }
        }
        MyClipboardWidgetSync.markDirty()
        MyClipboardWidgetSync.refreshAll(context)
        return phrases.size
    }

    fun hasBackup(): Boolean {
        val context = appContext ?: return false
        return context.getSharedPreferences(ClipboardBackupPrefsName, Context.MODE_PRIVATE)
            .contains(ClipboardBackupJsonKey)
    }

    fun getBackupSavedAtMillis(): Long? {
        val context = appContext ?: return null
        val prefs = context.getSharedPreferences(ClipboardBackupPrefsName, Context.MODE_PRIVATE)
        if (!prefs.contains(ClipboardBackupJsonKey)) {
            return null
        }
        val savedAtMillis = prefs.getLong(ClipboardBackupSavedAtKey, 0L)
        if (savedAtMillis > 0L) {
            return savedAtMillis
        }
        return prefs.getString(ClipboardBackupJsonKey, null)
            ?.toBackupSavedAtMillis()
            ?.takeIf { it > 0L }
    }

    suspend fun restoreBackup(): Int? {
        val context = appContext ?: return null
        val backupJson = context.getSharedPreferences(ClipboardBackupPrefsName, Context.MODE_PRIVATE)
            .getString(ClipboardBackupJsonKey, null)
            ?: return null
        val phrases = backupJson.toBackupPhrases() ?: return null

        val now = System.currentTimeMillis()
        database.clipboardPhraseDao().deleteAll()
        phrases.forEachIndexed { index, phrase ->
            database.clipboardPhraseDao().insert(
                phrase.copy(
                    id = 0,
                    updatedAt = now + (phrases.size - index),
                )
            )
        }
        MyClipboardWidgetSync.markDirty()
        MyClipboardWidgetSync.refreshAll(context)
        return phrases.size
    }

    private fun List<ClipboardPhraseEntity>.toBackupJson(savedAtMillis: Long): String {
        val items = JSONArray()
        forEach { phrase ->
            items.put(
                JSONObject()
                    .put("title", phrase.title)
                    .put("content", phrase.content)
                    .put("isFavorite", phrase.isFavorite)
            )
        }
        return JSONObject()
            .put("kind", ClipboardBackupKind)
            .put("version", 1)
            .put("savedAtMillis", savedAtMillis)
            .put("items", items)
            .toString()
    }

    private fun ClipboardPhraseEntity.backupIdentityKey(): String {
        return listOf(title, content, isFavorite.toString()).joinToString(separator = "\u0000")
    }

    private fun String.toBackupSavedAtMillis(): Long? {
        return runCatching {
            JSONObject(this).optLong("savedAtMillis", 0L)
        }.getOrNull()
    }

    private fun String.toBackupPhrases(): List<ClipboardPhraseEntity>? {
        return try {
            val backup = JSONObject(this)
            val kind = backup.optString("kind", ClipboardBackupKind)
            if (kind != ClipboardBackupKind) {
                return null
            }
            val items = backup.optJSONArray("items") ?: return null
            buildList<ClipboardPhraseEntity> {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val title = item.optString("title").trim()
                    val content = item.optString("content").trim()
                    if (title.isBlank() || content.isBlank()) {
                        continue
                    }
                    add(
                        ClipboardPhraseEntity(
                            title = title,
                            content = content,
                            isFavorite = item.optBoolean("isFavorite", false),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
