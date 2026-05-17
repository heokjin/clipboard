package com.soctt.myclipboard.ui

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soctt.myclipboard.R
import com.soctt.myclipboard.data.MaxReminderPreviewLineCount
import com.soctt.myclipboard.data.MinReminderPreviewLineCount
import com.soctt.myclipboard.data.local.ReminderEntity
import com.soctt.myclipboard.reminder.buildReminderAnnotatedString
import com.soctt.myclipboard.reminder.styleSpans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ReminderDeleteIntentMillis = 420L
private const val ReminderDeleteHoldMillis = 700L
private const val ReminderDeleteProgressResetMillis = 140
private const val WidgetDebugTag = "WidgetDebug"

@Composable
fun ReminderScreen(
    uiState: ReminderUiState,
    onSearchQueryChange: (String) -> Unit,
    onShowSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onShowWritingHintChange: (Boolean) -> Unit,
    onPinImportantToTopChange: (Boolean) -> Unit,
    onPreviewLineCountChange: (Int) -> Unit,
    onReminderInputChange: (TextFieldValue) -> Unit,
    onToggleHighlightSelection: () -> Unit,
    onDismissEditor: () -> Unit,
    onSaveReminder: () -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onSetReminderImportant: (ReminderEntity, Boolean) -> Unit,
    onDeleteAllReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isEditorVisible) {
        ReminderEditorScreen(
            uiState = uiState,
            onReminderInputChange = onReminderInputChange,
            onToggleHighlightSelection = onToggleHighlightSelection,
            onDismiss = onDismissEditor,
            onConfirm = onSaveReminder,
            modifier = modifier,
        )
    } else {
        ReminderListScreen(
            uiState = uiState,
            onSearchQueryChange = onSearchQueryChange,
            onShowSettings = onShowSettings,
            onShowWritingHintChange = onShowWritingHintChange,
            onEditReminder = onEditReminder,
            onDeleteReminder = onDeleteReminder,
            onSetReminderImportant = onSetReminderImportant,
            modifier = modifier,
        )
    }

    if (uiState.isSettingsVisible) {
        ReminderSettingsDialog(
            uiState = uiState,
            onDismiss = onDismissSettings,
            onShowWritingHintChange = onShowWritingHintChange,
            onPinImportantToTopChange = onPinImportantToTopChange,
            onPreviewLineCountChange = onPreviewLineCountChange,
            onDeleteAllReminders = onDeleteAllReminders,
        )
    }
}

@Composable
private fun ReminderListScreen(
    uiState: ReminderUiState,
    onSearchQueryChange: (String) -> Unit,
    onShowSettings: () -> Unit,
    onShowWritingHintChange: (Boolean) -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onSetReminderImportant: (ReminderEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.scrollToTopTick) {
        if (uiState.scrollToTopTick > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                label = {
                    Text(stringResource(R.string.search_label))
                },
                placeholder = {
                    Text(stringResource(R.string.reminder_search_placeholder))
                },
                singleLine = true,
            )
            TextButton(onClick = onShowSettings) {
                Text(stringResource(R.string.settings_action))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.showWritingHint) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.reminder_hint),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { onShowWritingHintChange(false) }) {
                    Text("X")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (uiState.reminders.isEmpty()) {
            ReminderEmptyState(
                searchQuery = uiState.searchQuery,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.reminders,
                    key = { reminder -> reminder.id },
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        previewLineCount = uiState.previewLineCount,
                        onEditReminder = onEditReminder,
                        onDeleteReminder = onDeleteReminder,
                        onSetImportant = onSetReminderImportant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderEmptyState(
    searchQuery: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (searchQuery.isBlank()) {
                    stringResource(R.string.reminder_empty_title)
                } else {
                    stringResource(R.string.empty_title_search)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (searchQuery.isBlank()) {
                    stringResource(R.string.reminder_empty_body)
                } else {
                    stringResource(R.string.reminder_empty_body_search)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    previewLineCount: Int,
    onEditReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onSetImportant: (ReminderEntity, Boolean) -> Unit,
) {
    val deleteProgress = remember(reminder.id) { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var isShowingDeleteProgress by remember(reminder.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(reminder.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = true)
                    var didDelete = false
                    var isDeleteMode = false

                    val progressJob = coroutineScope.launch {
                        deleteProgress.snapTo(0f)
                        delay(ReminderDeleteIntentMillis)
                        isDeleteMode = true
                        isShowingDeleteProgress = true
                        deleteProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = ReminderDeleteHoldMillis.toInt()),
                        )
                        didDelete = true
                        onDeleteReminder(reminder)
                    }

                    val up = waitForUpOrCancellation()

                    progressJob.cancel()

                    if (!didDelete) {
                        isShowingDeleteProgress = false
                        coroutineScope.launch {
                            deleteProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = ReminderDeleteProgressResetMillis),
                            )
                        }
                        if (up != null && !isDeleteMode) {
                            onEditReminder(reminder)
                        }
                    }
                }
            },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = buildReminderAnnotatedString(
                        text = reminder.text,
                        spans = reminder.styleSpans(),
                    ),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = previewLineCount,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = { onSetImportant(reminder, !reminder.isImportant) },
                    modifier = Modifier.heightIn(min = 28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) {
                    Text(
                        if (reminder.isImportant) {
                            "★"
                        } else {
                            "☆"
                        }
                    )
                }
            }

            if (isShowingDeleteProgress || deleteProgress.value > 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { deleteProgress.value },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.reminder_delete_progress_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ReminderSettingsDialog(
    uiState: ReminderUiState,
    onDismiss: () -> Unit,
    onShowWritingHintChange: (Boolean) -> Unit,
    onPinImportantToTopChange: (Boolean) -> Unit,
    onPreviewLineCountChange: (Int) -> Unit,
    onDeleteAllReminders: () -> Unit,
) {
    var isDeleteConfirmationVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.reminder_settings_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReminderSettingsSwitchRow(
                    title = stringResource(R.string.reminder_show_hint_setting_title),
                    description = stringResource(R.string.reminder_show_hint_setting_description),
                    checked = uiState.showWritingHint,
                    onCheckedChange = onShowWritingHintChange,
                )
                ReminderSettingsSwitchRow(
                    title = stringResource(R.string.reminder_pin_important_setting_title),
                    description = stringResource(R.string.reminder_pin_important_setting_description),
                    checked = uiState.pinImportantToTop,
                    onCheckedChange = onPinImportantToTopChange,
                )
                ReminderSettingsStepperRow(
                    title = stringResource(R.string.reminder_preview_lines_setting_title),
                    description = stringResource(R.string.reminder_preview_lines_setting_description),
                    value = uiState.previewLineCount,
                    onValueChange = onPreviewLineCountChange,
                )
                HorizontalDivider()
                TextButton(onClick = { isDeleteConfirmationVisible = true }) {
                    Text(stringResource(R.string.delete_all_reminders_action))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_action))
            }
        },
    )

    if (isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteConfirmationVisible = false },
            title = {
                Text(stringResource(R.string.delete_all_reminders_confirm_title))
            },
            text = {
                Text(stringResource(R.string.delete_all_reminders_confirm_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllReminders()
                        isDeleteConfirmationVisible = false
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteConfirmationVisible = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
}

@Composable
private fun ReminderSettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ReminderSettingsStepperRow(
    title: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(
                onClick = { onValueChange(value - 1) },
                enabled = value > MinReminderPreviewLineCount,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text("-")
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(
                onClick = { onValueChange(value + 1) },
                enabled = value < MaxReminderPreviewLineCount,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text("+")
            }
        }
    }
}

@Composable
private fun ReminderEditorScreen(
    uiState: ReminderUiState,
    onReminderInputChange: (TextFieldValue) -> Unit,
    onToggleHighlightSelection: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditing = uiState.editingReminderId != null
    val scrollState = rememberScrollState()
    LaunchedEffect(uiState.editingReminderId) {
        Log.d(
            WidgetDebugTag,
            "ReminderEditorScreen opened editingId=${uiState.editingReminderId} input='${uiState.reminderInputValue.text.trim().take(24)}'"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
            TextButton(
                onClick = onConfirm,
                enabled = uiState.isSaveEnabled,
            ) {
                Text(
                    if (isEditing) {
                        stringResource(R.string.update_action)
                    } else {
                        stringResource(R.string.save_action)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = uiState.reminderInputValue,
            onValueChange = onReminderInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
            label = {
                Text(stringResource(R.string.reminder_input_label))
            },
            placeholder = {
                Text(stringResource(R.string.reminder_input_placeholder))
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.reminder_formatting_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            FilterChip(
                selected = uiState.selectionStyle.highlighted,
                onClick = onToggleHighlightSelection,
                enabled = uiState.selectionStyle.hasSelection,
                label = {
                    Text(stringResource(R.string.reminder_format_bold))
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.reminder_preview_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SelectionContainer {
                if (uiState.reminderInputValue.text.isBlank()) {
                    Text(
                        text = stringResource(R.string.reminder_preview_placeholder),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = buildReminderAnnotatedString(
                            text = uiState.reminderInputValue.text,
                            spans = uiState.reminderStyleSpans,
                        ),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
