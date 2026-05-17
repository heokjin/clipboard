package com.soctt.myclipboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.soctt.myclipboard.data.ClipboardRepository
import com.soctt.myclipboard.data.ClipboardSettingsRepository
import com.soctt.myclipboard.data.ClipboardThemeMode
import com.soctt.myclipboard.data.local.ClipboardPhraseEntity
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

@OptIn(ExperimentalCoroutinesApi::class)
class ClipboardViewModel(
    private val repository: ClipboardRepository,
    private val settingsRepository: ClipboardSettingsRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val editorState = MutableStateFlow(EditorState())
    private val isSettingsVisible = MutableStateFlow(false)
    private var saveJob: Job? = null
    private val _copySuccessMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)

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

    val uiState: StateFlow<ClipboardUiState> = combine(
        searchQuery,
        phrases,
        editorState,
        settingsRepository.settings,
        isSettingsVisible,
    ) { query, phrases, editor, settings, isSettingsVisible ->
        ClipboardUiState(
            searchQuery = query,
            phrases = phrases,
            preventDuplicates = settings.preventDuplicates,
            themeMode = settings.themeMode,
            showCopySuccessMessage = settings.showCopySuccessMessage,
            copySuccessMessageTemplate = settings.copySuccessMessageTemplate,
            pinFavoritesToTop = settings.pinFavoritesToTop,
            previewLineCount = settings.previewLineCount,
            isSettingsVisible = isSettingsVisible,
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

    fun deleteAllPhrases() {
        viewModelScope.launch {
            repository.deleteAllPhrases()
            dismissEditor()
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
