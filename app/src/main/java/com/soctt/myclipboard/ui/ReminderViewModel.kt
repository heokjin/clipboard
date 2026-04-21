package com.soctt.myclipboard.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.soctt.myclipboard.data.ReminderRepository
import com.soctt.myclipboard.data.local.ReminderEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val WidgetDebugTag = "WidgetDebug"

class ReminderViewModel(
    private val repository: ReminderRepository,
) : ViewModel() {

    private val editorState = kotlinx.coroutines.flow.MutableStateFlow(EditorState())
    private var saveJob: Job? = null
    private val reminders = repository.observeReminders()

    val uiState: StateFlow<ReminderUiState> = combine(
        reminders,
        editorState,
    ) { reminders, editor ->
        ReminderUiState(
            reminders = reminders,
            isEditorVisible = editor.isVisible,
            editingReminderId = editor.editingReminderId,
            reminderInput = editor.input,
            isSaveEnabled = editor.input.isNotBlank(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReminderUiState(),
    )

    fun showAddDialog() {
        editorState.value = EditorState(
            isVisible = true,
            originalInput = "",
        )
    }

    fun dismissEditor() {
        editorState.value = EditorState()
    }

    fun handleEditorBack() {
        val editor = editorState.value
        val trimmedInput = editor.input.trim()
        val trimmedOriginal = editor.originalInput.trim()
        Log.d(
            WidgetDebugTag,
            "ReminderViewModel.handleEditorBack input='${trimmedInput.take(24)}' original='${trimmedOriginal.take(24)}' editingId=${editor.editingReminderId}"
        )

        if (trimmedInput.isBlank() || trimmedInput == trimmedOriginal) {
            Log.d(WidgetDebugTag, "ReminderViewModel.handleEditorBack -> dismiss only")
            dismissEditor()
            return
        }

        Log.d(WidgetDebugTag, "ReminderViewModel.handleEditorBack -> saveReminder")
        saveReminder()
    }

    fun onReminderInputChange(text: String) {
        editorState.value = editorState.value.copy(input = text)
    }

    fun startEdit(reminder: ReminderEntity) {
        editorState.value = EditorState(
            isVisible = true,
            editingReminderId = reminder.id,
            input = reminder.text,
            originalInput = reminder.text,
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
        val text = editor.input.trim()
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
        }
    }

    companion object {
        fun factory(repository: ReminderRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    ReminderViewModel(repository)
                }
            }
        }
    }

    private data class EditorState(
        val isVisible: Boolean = false,
        val editingReminderId: Long? = null,
        val input: String = "",
        val originalInput: String = "",
    )

    private suspend fun saveEditor(
        editor: EditorState,
        dismissWhenUnchanged: Boolean,
    ) {
        val text = editor.input.trim()
        if (text.isBlank()) {
            Log.d(WidgetDebugTag, "ReminderViewModel.saveEditor skipped because blank")
            return
        }

        val hasChanges = editor.editingReminderId == null ||
            text != editor.originalInput.trim()
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
        if (editingReminderId == null) {
            Log.d(WidgetDebugTag, "ReminderViewModel.saveEditor -> addReminder")
            repository.addReminder(text)
        } else {
            Log.d(WidgetDebugTag, "ReminderViewModel.saveEditor -> updateReminder id=$editingReminderId")
            repository.updateReminder(editingReminderId, text)
        }
        dismissEditor()
        Log.d(WidgetDebugTag, "ReminderViewModel.saveEditor -> dismissEditor")
    }
}
