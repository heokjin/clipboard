package com.soctt.myclipboard.ui

import androidx.compose.ui.text.input.TextFieldValue
import com.soctt.myclipboard.data.local.ReminderEntity
import com.soctt.myclipboard.reminder.ReminderSelectionStyle
import com.soctt.myclipboard.reminder.ReminderStyleSpan

data class ReminderUiState(
    val searchQuery: String = "",
    val reminders: List<ReminderEntity> = emptyList(),
    val scrollToTopTick: Int = 0,
    val showWritingHint: Boolean = true,
    val pinImportantToTop: Boolean = true,
    val previewLineCount: Int = 2,
    val isSettingsVisible: Boolean = false,
    val isEditorVisible: Boolean = false,
    val editingReminderId: Long? = null,
    val reminderInputValue: TextFieldValue = TextFieldValue(""),
    val reminderStyleSpans: List<ReminderStyleSpan> = emptyList(),
    val selectionStyle: ReminderSelectionStyle = ReminderSelectionStyle(
        hasSelection = false,
        highlighted = false,
    ),
    val isSaveEnabled: Boolean = false,
)
