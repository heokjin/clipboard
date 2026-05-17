package com.soctt.myclipboard.data

import android.content.Context
import com.soctt.myclipboard.data.local.ClipboardDatabase
import com.soctt.myclipboard.data.local.ClipboardPhraseEntity
import com.soctt.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.flow.Flow

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
}
