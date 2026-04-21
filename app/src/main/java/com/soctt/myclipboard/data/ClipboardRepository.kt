package com.soctt.myclipboard.data

import android.content.Context
import com.soctt.myclipboard.data.local.ClipboardDatabase
import com.soctt.myclipboard.data.local.ClipboardPhraseEntity
import com.soctt.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.flow.Flow

class ClipboardRepository(
    private val database: ClipboardDatabase,
) {
    constructor(context: Context) : this(
        database = ClipboardDatabase.getDatabase(context),
    )

    fun observePhrases(query: String): Flow<List<ClipboardPhraseEntity>> {
        return database.clipboardPhraseDao().observePhrases(query.trim())
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

    suspend fun addPhrase(title: String, content: String) {
        database.clipboardPhraseDao().insert(
            ClipboardPhraseEntity(
                title = title,
                content = content,
            )
        )
        MyClipboardWidgetSync.markDirty()
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
        MyClipboardWidgetSync.markDirty()
    }

    suspend fun deletePhrase(phrase: ClipboardPhraseEntity) {
        database.clipboardPhraseDao().delete(phrase)
        MyClipboardWidgetSync.markDirty()
    }
}
