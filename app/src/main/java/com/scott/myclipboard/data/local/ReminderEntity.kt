package com.scott.myclipboard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val styleSpansJson: String = "[]",
    val isImportant: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
