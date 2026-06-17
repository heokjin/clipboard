package com.scott.myclipboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.scott.myclipboard.data.ClipboardRepository
import com.scott.myclipboard.data.ClipboardSettingsRepository
import com.scott.myclipboard.data.ClipboardThemeMode
import com.scott.myclipboard.data.local.ClipboardPhraseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ClipboardSettingsEvent {
    data class BackupSaved(val count: Int) : ClipboardSettingsEvent
    data class BackupRestored(val count: Int) : ClipboardSettingsEvent
    data class BackupImported(val count: Int) : ClipboardSettingsEvent
    data object BackupUnavailable : ClipboardSettingsEvent
    data object BackupImportFailed : ClipboardSettingsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ClipboardViewModel(
    private val repository: ClipboardRepository,
    private val settingsRepository: ClipboardSettingsRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val editorState = MutableStateFlow(EditorState())
    private val isSettingsVisible = MutableStateFlow(false)
    private val hasBackup = MutableStateFlow(repository.hasBackup())
    private val backupSavedAtMillis = MutableStateFlow(repository.getBackupSavedAtMillis())
    private var saveJob: Job? = null
    private val _copySuccessMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val _settingsEvents = MutableSharedFlow<ClipboardSettingsEvent>(extraBufferCapacity = 1)

    private val phrases = combine(
        searchQuery,
        settingsRepository.settings,
    ) { query, settings ->
        query to settings.pinFavoritesToTop
    }.flatMapLatest { (query, pinFavoritesToTop) ->
        repository.observePhrases(
            query = query,
            pinFavoritesToTop = pinFavoritesToTop,
        )
    }
    private val settingsUiState = combine(
        settingsRepository.settings,
        isSettingsVisible,
        hasBackup,
        backupSavedAtMillis,
    ) { settings, isSettingsVisible, hasBackup, backupSavedAtMillis ->
        ClipboardSettingsUiState(
            settings = settings,
            isSettingsVisible = isSettingsVisible,
            hasBackup = hasBackup,
            backupSavedAtMillis = backupSavedAtMillis,
        )
    }

    val uiState: StateFlow<ClipboardUiState> = combine(
        searchQuery,
        phrases,
        editorState,
        settingsUiState,
    ) { query, phrases, editor, settingsUiState ->
        val settings = settingsUiState.settings
        ClipboardUiState(
            searchQuery = query,
            phrases = phrases,
            preventDuplicates = settings.preventDuplicates,
            themeMode = settings.themeMode,
            showCopySuccessMessage = settings.showCopySuccessMessage,
            copySuccessMessageTemplate = settings.copySuccessMessageTemplate,
            pinFavoritesToTop = settings.pinFavoritesToTop,
            previewLineCount = settings.previewLineCount,
            widgetFontSize = settings.widgetFontSize,
            hasBackup = settingsUiState.hasBackup,
            backupSavedAtMillis = settingsUiState.backupSavedAtMillis,
            isSettingsVisible = settingsUiState.isSettingsVisible,
            isEditorVisible = editor.isVisible,
            editingPhraseId = editor.editingPhraseId,
            titleInput = editor.titleInput,
            contentInput = editor.contentInput,
            isSaveEnabled = editor.titleInput.isNotBlank() && editor.contentInput.isNotBlank(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClipboardUiState(),
    )

    val copySuccessMessages: SharedFlow<String> = _copySuccessMessages.asSharedFlow()
    val settingsEvents: SharedFlow<ClipboardSettingsEvent> = _settingsEvents.asSharedFlow()

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun showAddDialog() {
        editorState.value = EditorState(
            isVisible = true,
            originalTitle = "",
            originalContent = "",
        )
    }

    fun dismissEditor() {
        editorState.value = EditorState()
    }

    fun showSettings() {
        isSettingsVisible.value = true
    }

    fun dismissSettings() {
        isSettingsVisible.value = false
    }

    fun setPreventDuplicates(enabled: Boolean) {
        settingsRepository.setPreventDuplicates(enabled)
    }

    fun setThemeMode(themeMode: ClipboardThemeMode) {
        settingsRepository.setThemeMode(themeMode)
    }

    fun setShowCopySuccessMessage(enabled: Boolean) {
        settingsRepository.setShowCopySuccessMessage(enabled)
    }

    fun setCopySuccessMessageTemplate(template: String) {
        settingsRepository.setCopySuccessMessageTemplate(template)
    }

    fun setPinFavoritesToTop(enabled: Boolean) {
        settingsRepository.setPinFavoritesToTop(enabled)
    }

    fun setPreviewLineCount(lineCount: Int) {
        settingsRepository.setPreviewLineCount(lineCount)
    }

    fun setWidgetFontSize(fontSize: Int) {
        settingsRepository.setWidgetFontSize(fontSize)
    }

    fun deleteAllPhrases() {
        viewModelScope.launch {
            repository.deleteAllPhrases()
            dismissEditor()
        }
    }

    fun saveCurrentBackup() {
        viewModelScope.launch {
            val count = repository.saveCurrentBackup()
            hasBackup.value = repository.hasBackup()
            backupSavedAtMillis.value = repository.getBackupSavedAtMillis()
            _settingsEvents.tryEmit(ClipboardSettingsEvent.BackupSaved(count))
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            val count = repository.restoreBackup()
            if (count != null) {
                _settingsEvents.tryEmit(ClipboardSettingsEvent.BackupRestored(count))
            } else {
                _settingsEvents.tryEmit(ClipboardSettingsEvent.BackupUnavailable)
            }
        }
    }

    fun getBackupExportJson(): String? {
        return repository.getBackupExportJson()
    }

    fun importBackupJson(backupJson: String) {
        viewModelScope.launch {
            val count = repository.importBackupJson(backupJson)
            if (count != null) {
                hasBackup.value = repository.hasBackup()
                backupSavedAtMillis.value = repository.getBackupSavedAtMillis()
                _settingsEvents.tryEmit(ClipboardSettingsEvent.BackupImported(count))
            } else {
                _settingsEvents.tryEmit(ClipboardSettingsEvent.BackupImportFailed)
            }
        }
    }

    fun onTitleChange(title: String) {
        editorState.value = editorState.value.copy(titleInput = title)
    }

    fun onContentChange(content: String) {
        editorState.value = editorState.value.copy(contentInput = content)
    }

    fun startEdit(phrase: ClipboardPhraseEntity) {
        editorState.value = EditorState(
            isVisible = true,
            editingPhraseId = phrase.id,
            titleInput = phrase.title,
            contentInput = phrase.content,
            originalTitle = phrase.title,
            originalContent = phrase.content,
        )
    }

    fun savePhrase() {
        val editor = editorState.value
        val title = editor.titleInput.trim()
        val content = editor.contentInput.trim()
        if (title.isBlank() || content.isBlank() || saveJob?.isActive == true) {
            return
        }

        saveJob = viewModelScope.launch {
            try {
                saveEditor(editor, dismissWhenUnchanged = true)
            } finally {
                saveJob = null
            }
        }
    }

    suspend fun persistEditorIfNeeded() {
        saveJob?.join()
        val editor = editorState.value
        if (!editor.isVisible) {
            return
        }

        saveEditor(editor, dismissWhenUnchanged = false)
    }

    fun deletePhrase(phrase: ClipboardPhraseEntity) {
        viewModelScope.launch {
            repository.deletePhrase(phrase)
            if (editorState.value.editingPhraseId == phrase.id) {
                dismissEditor()
            }
        }
    }

    fun setPhraseFavorite(phrase: ClipboardPhraseEntity, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.setPhraseFavorite(
                id = phrase.id,
                isFavorite = isFavorite,
            )
        }
    }

    fun notifyCopySuccess(phrase: ClipboardPhraseEntity) {
        val label = phrase.title.ifBlank {
            phrase.content.lineSequence().firstOrNull().orEmpty().take(24)
        }
        _copySuccessMessages.tryEmit(label)
    }

    companion object {
        fun factory(
            repository: ClipboardRepository,
            settingsRepository: ClipboardSettingsRepository,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    ClipboardViewModel(
                        repository = repository,
                        settingsRepository = settingsRepository,
                    )
                }
            }
        }
    }

    private data class EditorState(
        val isVisible: Boolean = false,
        val editingPhraseId: Long? = null,
        val titleInput: String = "",
        val contentInput: String = "",
        val originalTitle: String = "",
        val originalContent: String = "",
    )

    private data class ClipboardSettingsUiState(
        val settings: com.scott.myclipboard.data.ClipboardSettings,
        val isSettingsVisible: Boolean,
        val hasBackup: Boolean,
        val backupSavedAtMillis: Long?,
    )

    private suspend fun saveEditor(
        editor: EditorState,
        dismissWhenUnchanged: Boolean,
    ) {
        val title = editor.titleInput.trim()
        val content = editor.contentInput.trim()
        if (title.isBlank() || content.isBlank()) {
            return
        }

        val hasChanges = editor.editingPhraseId == null ||
            title != editor.originalTitle.trim() ||
            content != editor.originalContent.trim()

        if (!hasChanges) {
            if (dismissWhenUnchanged) {
                dismissEditor()
            }
            return
        }

        val editingPhraseId = editor.editingPhraseId
        if (editingPhraseId == null) {
            repository.addPhrase(
                title = title,
                content = content,
                preventDuplicates = settingsRepository.settings.value.preventDuplicates,
            )
        } else {
            repository.updatePhrase(
                id = editingPhraseId,
                title = title,
                content = content,
            )
        }
        dismissEditor()
    }
}
