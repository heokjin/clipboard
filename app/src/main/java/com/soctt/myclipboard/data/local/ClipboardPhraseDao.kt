package com.soctt.myclipboard.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardPhraseDao {
    @Query(
        """
        SELECT * FROM clipboard_phrases
        WHERE :query = ''
            OR title LIKE '%' || :query || '%'
            OR content LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun observePhrases(query: String): Flow<List<ClipboardPhraseEntity>>

    @Query(
        """
        SELECT * FROM clipboard_phrases
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentPhrases(limit: Int): List<ClipboardPhraseEntity>

    @Query(
        """
        SELECT * FROM clipboard_phrases
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getPhraseById(id: Long): ClipboardPhraseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: ClipboardPhraseEntity): Long

    @Update
    suspend fun update(phrase: ClipboardPhraseEntity)

    @Delete
    suspend fun delete(phrase: ClipboardPhraseEntity)
}
