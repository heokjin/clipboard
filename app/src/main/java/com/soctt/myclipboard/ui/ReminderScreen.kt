package com.soctt.myclipboard.ui

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soctt.myclipboard.R
import com.soctt.myclipboard.data.local.ReminderEntity
import kotlinx.coroutines.launch

private const val ReminderDeleteHoldMillis = 1_000L
private const val ReminderDeleteProgressResetMillis = 140
private const val WidgetDebugTag = "WidgetDebug"

@Composable
fun ReminderScreen(
    uiState: ReminderUiState,
    onReminderInputChange: (String) -> Unit,
    onDismissEditor: () -> Unit,
    onSaveReminder: () -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isEditorVisible) {
        ReminderEditorScreen(
            uiState = uiState,
            onReminderInputChange = onReminderInputChange,
            onDismiss = onDismissEditor,
            onConfirm = onSaveReminder,
            modifier = modifier,
        )
    } else {
        ReminderListScreen(
            uiState = uiState,
            onEditReminder = onEditReminder,
            onDeleteReminder = onDeleteReminder,
            modifier = modifier,
        )
    }
}

@Composable
private fun ReminderListScreen(
    uiState: ReminderUiState,
    onEditReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.reminder_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.reminders.isEmpty()) {
            ReminderEmptyState(modifier = Modifier.fillMaxWidth())
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.reminders,
                    key = { reminder -> reminder.id },
                ) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onEditReminder = onEditReminder,
                        onDeleteReminder = onDeleteReminder,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderEmptyState(
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.reminder_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reminder_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderEntity,
    onEditReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
) {
    val deleteProgress = remember(reminder.id) { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var isShowingDeleteProgress by remember(reminder.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(reminder.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var didDelete = false
                    isShowingDeleteProgress = true

                    val progressJob = coroutineScope.launch {
                        deleteProgress.snapTo(0f)
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
                        if (up != null) {
                            onEditReminder(reminder)
                        }
                    }
                }
            },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Text(
                text = reminder.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

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
private fun ReminderEditorScreen(
    uiState: ReminderUiState,
    onReminderInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditing = uiState.editingReminderId != null
    LaunchedEffect(uiState.editingReminderId) {
        Log.d(
            WidgetDebugTag,
            "ReminderEditorScreen opened editingId=${uiState.editingReminderId} input='${uiState.reminderInput.trim().take(24)}'"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
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

        Text(
            text = stringResource(R.string.reminder_editor_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.reminderInput,
            onValueChange = onReminderInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = {
                Text(stringResource(R.string.reminder_input_label))
            },
            placeholder = {
                Text(stringResource(R.string.reminder_input_placeholder))
            },
        )
    }
}
