package com.soctt.myclipboard.ui

import com.soctt.myclipboard.data.local.ReminderEntity

data class ReminderUiState(
    val reminders: List<ReminderEntity> = emptyList(),
    val isEditorVisible: Boolean = false,
    val editingReminderId: Long? = null,
    val reminderInput: String = "",
    val isSaveEnabled: Boolean = false,
)
