package com.soctt.myclipboard.ui

import com.soctt.myclipboard.data.ClipboardThemeMode
import com.soctt.myclipboard.data.local.ClipboardPhraseEntity

data class ClipboardUiState(
    val searchQuery: String = "",
    val phrases: List<ClipboardPhraseEntity> = emptyList(),
    val preventDuplicates: Boolean = true,
    val themeMode: ClipboardThemeMode = ClipboardThemeMode.System,
    val showCopySuccessMessage: Boolean = true,
    val copySuccessMessageTemplate: String = "",
    val pinFavoritesToTop: Boolean = true,
    val previewLineCount: Int = 2,
    val widgetFontSize: Int = 12,
    val isSettingsVisible: Boolean = false,
    val isEditorVisible: Boolean = false,
    val editingPhraseId: Long? = null,
    val titleInput: String = "",
    val contentInput: String = "",
    val isSaveEnabled: Boolean = false,
)
