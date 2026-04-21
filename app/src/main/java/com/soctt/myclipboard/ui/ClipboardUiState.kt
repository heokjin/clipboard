package com.soctt.myclipboard.ui

import com.soctt.myclipboard.data.local.ClipboardPhraseEntity

data class ClipboardUiState(
    val searchQuery: String = "",
    val phrases: List<ClipboardPhraseEntity> = emptyList(),
    val isEditorVisible: Boolean = false,
    val editingPhraseId: Long? = null,
    val titleInput: String = "",
    val contentInput: String = "",
    val isSaveEnabled: Boolean = false,
)
