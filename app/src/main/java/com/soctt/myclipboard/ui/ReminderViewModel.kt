package com.soctt.myclipboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.soctt.myclipboard.data.ReminderRepository
import com.soctt.myclipboard.data.local.ReminderEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val repository: ReminderRepository,
) : ViewModel() {

    private val editorState = kotlinx.coroutines.flow.MutableStateFlow(EditorState())
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

        if (trimmedInput.isBlank() || trimmedInput == trimmedOriginal) {
            dismissEditor()
            return
        }

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
        if (text.isBlank()) {
            return
        }

        viewModelScope.launch {
            saveEditor(editor, dismissWhenUnchanged = true)
        }
    }

    suspend fun persistEditorIfNeeded() {
        val editor = editorState.value
        if (!editor.isVisible) {
            return
        }

        saveEditor(editor, dismissWhenUnchanged = false)
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
            return
        }

        val hasChanges = editor.editingReminderId == null ||
            text != editor.originalInput.trim()

        if (!hasChanges) {
            if (dismissWhenUnchanged) {
                dismissEditor()
            }
            return
        }

        val editingReminderId = editor.editingReminderId
        if (editingReminderId == null) {
            repository.addReminder(text)
        } else {
            repository.updateReminder(editingReminderId, text)
        }
        dismissEditor()
    }
}
