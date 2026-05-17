package com.soctt.myclipboard.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query(
        """
        SELECT * FROM reminders
        WHERE :query = ''
            OR text LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN :pinImportantToTop THEN isImportant ELSE 0 END DESC,
            updatedAt DESC
        """
    )
    fun observeReminders(
        query: String,
        pinImportantToTop: Boolean,
    ): Flow<List<ReminderEntity>>

    @Query(
        """
        SELECT * FROM reminders
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentReminders(limit: Int): Flow<List<ReminderEntity>>

    @Query(
        """
        SELECT * FROM reminders
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentReminders(limit: Int): List<ReminderEntity>

    @Query(
        """
        SELECT * FROM reminders
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
