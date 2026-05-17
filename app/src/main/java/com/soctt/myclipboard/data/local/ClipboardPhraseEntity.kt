package com.soctt.myclipboard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_phrases")
data class ClipboardPhraseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
