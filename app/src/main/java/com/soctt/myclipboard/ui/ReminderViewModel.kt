package com.soctt.myclipboard.ui

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.soctt.myclipboard.data.ReminderRepository
import com.soctt.myclipboard.data.ReminderSettingsRepository
import com.soctt.myclipboard.data.local.ReminderEntity
import com.soctt.myclipboard.reminder.ReminderStyleSpan
import com.soctt.myclipboard.reminder.adjustReminderSpansForTextChange
import com.soctt.myclipboard.reminder.encodeReminderStyleSpans
import com.soctt.myclipboard.reminder.styleSpans
import com.soctt.myclipboard.reminder.summarizeReminderSelectionStyle
import com.soctt.myclipboard.reminder.toggleReminderHighlightOnSelection
import com.soctt.myclipboard.reminder.trimReminderTextAndSpans
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val WidgetDebugTag = "WidgetDebug"

sealed interface ReminderSettingsEvent {
    data class BackupSaved(val count: Int) : ReminderSettingsEvent
    data class BackupRestored(val count: Int) : ReminderSettingsEvent
    data object BackupUnavailable : ReminderSettingsEvent
}

private fun mergedSelectionCandidate(
    current: TextRange?,
    previous: TextRange?,
): TextRange? {
    return when {
        current == null -> previous
        previous == null -> current
        current.canMergeWith(previous) -> TextRange(
            start = minOf(current.start, previous.start),
            end = maxOf(current.end, previous.end),
        )
        else -> current
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModel(
    private val repository: ReminderRepository,
    private val settingsRepository: ReminderSettingsRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val scrollToTopTick = MutableStateFlow(0)
    private val editorState = MutableStateFlow(EditorState())
    private val isSettingsVisible = MutableStateFlow(false)
    private val hasBackup = MutableStateFlow(repository.hasBackup())
    private var saveJob: Job? = null
    private val _deletedReminderEvents = MutableSharedFlow<ReminderEntity>(extraBufferCapacity = 1)
    private val _settingsEvents = MutableSharedFlow<ReminderSettingsEvent>(extraBufferCapacity = 1)
    private val reminders = combine(
        searchQuery,
        settingsRepository.settings,
    ) { query, settings ->
        query to settings.pinImportantToTop
    }.flatMapLatest { (query, pinImportantToTop) ->
        repository.observeReminders(
            query = query,
            pinImportantToTop = pinImportantToTop,
        )
    }
    private val settingsUiState = combine(
        settingsRepository.settings,
        isSettingsVisible,
        hasBackup,
    ) { settings, isSettingsVisible, hasBackup ->
        ReminderSettingsUiState(
            settings = settings,
            isSettingsVisible = isSettingsVisible,
            hasBackup = hasBackup,
        )
    }

    val uiState: StateFlow<ReminderUiState> = combine(
        searchQuery,
        scrollToTopTick,
        reminders,
        editorState,
        settingsUiState,
    ) { query, scrollTick, reminders, editor, settingsUiState ->
        val settings = settingsUiState.settings
        ReminderUiState(
            searchQuery = query,
            scrollToTopTick = scrollTick,
            reminders = reminders,
            showWritingHint = settings.showWritingHint,
            pinImportantToTop = settings.pinImportantToTop,
            previewLineCount = settings.previewLineCount,
            widgetFontSize = settings.widgetFontSize,
            hasBackup = settingsUiState.hasBackup,
            isSettingsVisible = settingsUiState.isSettingsVisible,
            isEditorVisible = editor.isVisible,
            editingReminderId = editor.editingReminderId,
            reminderInputValue = editor.inputValue,
            reminderStyleSpans = editor.styleSpans,
            selectionStyle = summarizeReminderSelectionStyle(
                text = editor.inputValue.text,
                spans = editor.styleSpans,
                selection = editor.activeSelection(),
            ),
            isSaveEnabled = editor.inputValue.text.isNotBlank(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReminderUiState(),
    )

    val deletedReminderEvents: SharedFlow<ReminderEntity> = _deletedReminderEvents.asSharedFlow()
    val settingsEvents: SharedFlow<ReminderSettingsEvent> = _settingsEvents.asSharedFlow()

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun showAddDialog() {
        editorState.value = EditorState(
            isVisible = true,
            originalText = "",
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

    fun setShowWritingHint(enabled: Boolean) {
        settingsRepository.setShowWritingHint(enabled)
    }

    fun setPinImportantToTop(enabled: Boolean) {
        settingsRepository.setPinImportantToTop(enabled)
    }

    fun setPreviewLineCount(lineCount: Int) {
        settingsRepository.setPreviewLineCount(lineCount)
    }

    fun setWidgetFontSize(fontSize: Int) {
        settingsRepository.setWidgetFontSize(fontSize)
    }

    fun handleEditorBack() {
        val editor = editorState.value
        val (trimmedInput, trimmedSpans) = trimReminderTextAndSpans(
            text = editor.inputValue.text,
            spans = editor.styleSpans,
        )
        val (trimmedOriginal, trimmedOriginalSpans) = trimReminderTextAndSpans(
            text = editor.originalText,
            spans = editor.originalStyleSpans,
        )
        Log.d(
            WidgetDebugTag,
            "ReminderViewModel.handleEditorBack input='${trimmedInput.take(24)}' original='${trimmedOriginal.take(24)}' editingId=${editor.editingReminderId}"
        )

        if (trimmedInput.isBlank() || (trimmedInput == trimmedOriginal && trimmedSpans == trimmedOriginalSpans)) {
            Log.d(WidgetDebugTag, "ReminderViewModel.handleEditorBack -> dismiss only")
            dismissEditor()
            return
        }

        Log.d(WidgetDebugTag, "ReminderViewModel.handleEditorBack -> saveReminder")
        saveReminder()
    }

    fun onReminderInputChange(inputValue: TextFieldValue) {
        val previous = editorState.value
        val nextSpans = adjustReminderSpansForTextChange(
            oldText = previous.inputValue.text,
            newText = inputValue.text,
            spans = previous.styleSpans,
        )
        val nextSelection = mergedSelectionCandidate(
            current = inputValue.selection.takeIf { !it.collapsed },
            previous = previous.lastNonCollapsedSelection,
        )
        editorState.value = previous.copy(
            inputValue = inputValue,
            styleSpans = nextSpans,
            lastNonCollapsedSelection = nextSelection,
            lastComposition = inputValue.composition ?: previous.lastComposition,
        )
    }

    fun toggleHighlightOnSelection() {
        val editor = editorState.value
        editorState.value = editor.copy(
            styleSpans = toggleReminderHighlightOnSelection(
                text = editor.inputValue.text,
                spans = editor.styleSpans,
                selection = editor.activeSelection(),
            )
        )
    }

    fun startEdit(reminder: ReminderEntity) {
        editorState.value = EditorState(
            isVisible = true,
            editingReminderId = reminder.id,
            inputValue = TextFieldValue(reminder.text),
            originalText = reminder.text,
            styleSpans = reminder.styleSpans(),
            originalStyleSpans = reminder.styleSpans(),
            lastNonCollapsedSelection = null,
        )
    }

    fun startEditById(reminderId: Long) {
        viewModelScope.launch {
            repository.getReminderById(reminderId)?.let { reminder ->
                startEdit(reminder)
            }
        }
    }

    fun saveReminder() {
        val editor = editorState.value
        val (text, _) = trimReminderTextAndSpans(
            text = editor.inputValue.text,
            spans = editor.styleSpans,
        )
        if (text.isBlank() || saveJob?.isActive == true) {
            Log.d(
                WidgetDebugTag,
                "ReminderViewModel.saveReminder skipped blank=${text.isBlank()} saveRunning=${saveJob?.isActive == true}"
            )
            return
        }

        saveJob = viewModelScope.launch {
            try {
                Log.d(WidgetDebugTag, "ReminderViewModel.saveReminder started editingId=${editor.editingReminderId}")
                saveEditor(editor, dismissWhenUnchanged = true)
                Log.d(WidgetDebugTag, "ReminderViewModel.saveReminder completed editingId=${editor.editingReminderId}")
            } finally {
                saveJob = null
            }
        }
    }

    suspend fun persistEditorIfNeeded() {
        Log.d(
            WidgetDebugTag,
            "ReminderViewModel.persistEditorIfNeeded editorVisible=${editorState.value.isVisible} saveRunning=${saveJob?.isActive == true}"
        )
        saveJob?.join()
        val editor = editorState.value
        if (!editor.isVisible) {
            Log.d(WidgetDebugTag, "ReminderViewModel.persistEditorIfNeeded -> no visible editor")
            return
        }

        saveEditor(editor, dismissWhenUnchanged = false)
        Log.d(WidgetDebugTag, "ReminderViewModel.persistEditorIfNeeded -> saveEditor finished")
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            if (editorState.value.editingReminderId == reminder.id) {
                dismissEditor()
            }
            _deletedReminderEvents.tryEmit(reminder)
        }
    }

    fun setReminderImportant(reminder: ReminderEntity, isImportant: Boolean) {
        viewModelScope.launch {
            repository.setReminderImportant(
                id = reminder.id,
                isImportant = isImportant,
            )
        }
    }

    fun deleteAllReminders() {
        viewModelScope.launch {
            repository.deleteAllReminders()
            dismissEditor()
        }
    }

    fun saveCurrentBackup() {
        viewModelScope.launch {
            val count = repository.saveCurrentBackup()
            hasBackup.value = repository.hasBackup()
            _settingsEvents.tryEmit(ReminderSettingsEvent.BackupSaved(count))
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            val count = repository.restoreBackup()
            if (count != null) {
                _settingsEvents.tryEmit(ReminderSettingsEvent.BackupRestored(count))
                scrollToTopTick.value += 1
            } else {
                _settingsEvents.tryEmit(ReminderSettingsEvent.BackupUnavailable)
            }
        }
    }

    fun restoreReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.restoreReminder(reminder)
            scrollToTopTick.value += 1
        }
    }

    companion object {
        fun factory(
            repository: ReminderRepository,
            settingsRepository: ReminderSettingsRepository,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    ReminderViewModel(
                        repository = repository,
                        settingsRepository = settingsRepository,
                    )
                }
            }
        }
    }

    private data class EditorState(
        val isVisible: Boolean = false,
        val editingReminderId: Long? = null,
        val inputValue: TextFieldValue = TextFieldValue(""),
        val originalText: String = "",
        val styleSpans: List<ReminderStyleSpan> = emptyList(),
        val originalStyleSpans: List<ReminderStyleSpan> = emptyList(),
        val lastNonCollapsedSelection: TextRange? = null,
        val lastComposition: TextRange? = null,
    ) {
        fun activeSelection(): TextRange {
            val currentSelection = inputValue.selection.takeIf { !it.collapsed }
            val baseSelection = mergedSelectionCandidate(
                current = currentSelection,
                previous = lastNonCollapsedSelection,
            ) ?: inputValue.selection
            val composition = inputValue.composition ?: lastComposition
            return if (composition != null && baseSelection.canMergeWith(composition)) {
                TextRange(
                    start = minOf(baseSelection.start, composition.start),
                    end = maxOf(baseSelection.end, composition.end),
                )
            } else {
                baseSelection
            }
        }
    }

    private data class ReminderSettingsUiState(
        val settings: com.soctt.myclipboard.data.ReminderSettings,
        val isSettingsVisible: Boolean,
        val hasBackup: Boolean,
    )

    private suspend fun saveEditor(
        editor: EditorState,
        dismissWhenUnchanged: Boolean,
    ) {
        val (text, styleSpans) = trimReminderTextAndSpans(
            text = editor.inputValue.text,
            spans = editor.styleSpans,
        )
        if (text.isBlank()) {
            Log.d(WidgetDebugTag, "ReminderViewModel.saveEditor skipped because blank")
            return
        }

        val (originalText, originalStyleSpans) = trimReminderTextAndSpans(
            text = editor.originalText,
            spans = editor.originalStyleSpans,
        )
        val hasChanges = editor.editingReminderId == null ||
            text != originalText ||
            styleSpans != originalStyleSpans
        Log.d(
            WidgetDebugTag,
            "ReminderViewModel.saveEditor editingId=${editor.editingReminderId} hasChanges=$hasChanges dismissWhenUnchanged=$dismissWhenUnchanged"
        )

        if (!hasChanges) {
            if (dismissWhenUnchanged) {
                dismissEditor()
            }
            Log.d(WidgetDebugTag, "ReminderViewModel.saveEditor -> unchanged")
            return
        }

        val editingReminderId = editor.editingReminderId
        val styleSpansJson = encodeReminderStyleSpans(styleSpans)
        if (editingReminderId == null) {
            Log.d(WidgetDebugTag, "ReminderViewModel.saveEditor -> addReminder")
            repository.addReminder(text, styleSpansJson)
        } else {
            Log.d(WidgetDebugTag, "ReminderViewModel.saveEditor -> updateReminder id=$editingReminderId")
            repository.updateReminder(editingReminderId, text, styleSpansJson)
        }
        dismissEditor()
        Log.d(WidgetDebugTag, "ReminderViewModel.saveEditor -> dismissEditor")
    }
}

private fun TextRange.canMergeWith(other: TextRange): Boolean {
    val rangeStart = minOf(this.start, this.end)
    val rangeEnd = maxOf(this.start, this.end)
    val otherStart = minOf(other.start, other.end)
    val otherEnd = maxOf(other.start, other.end)
    return otherStart <= rangeEnd && otherEnd >= rangeStart
}
