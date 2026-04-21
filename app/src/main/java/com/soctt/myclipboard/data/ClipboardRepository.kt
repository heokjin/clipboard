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

    fun observePhrases(query: String): Flow<List<ClipboardPhraseEntity>> {
        return database.clipboardPhraseDao().observePhrases(query.trim())
    }

    suspend fun getRecentPhrases(limit: Int): List<ClipboardPhraseEntity> {
        return database.clipboardPhraseDao().getRecentPhrases(limit)
    }

    suspend fun getPhraseById(id: Long): ClipboardPhraseEntity? {
        return database.clipboardPhraseDao().getPhraseById(id)
    }

    suspend fun addPhrase(title: String, content: String) {
        database.clipboardPhraseDao().insert(
            ClipboardPhraseEntity(
                title = title,
                content = content,
            )
        )
        appContext?.let { MyClipboardWidgetSync.refreshAll(it) }
    }

    suspend fun updatePhrase(id: Long, title: String, content: String) {
        database.clipboardPhraseDao().update(
            ClipboardPhraseEntity(
                id = id,
                title = title,
                content = content,
                updatedAt = System.currentTimeMillis(),
            )
        )
        appContext?.let { MyClipboardWidgetSync.refreshAll(it) }
    }

    suspend fun deletePhrase(phrase: ClipboardPhraseEntity) {
        database.clipboardPhraseDao().delete(phrase)
        appContext?.let { MyClipboardWidgetSync.refreshAll(it) }
    }
}
