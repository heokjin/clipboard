package com.scott.myclipboard.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scott.myclipboard.R
import com.scott.myclipboard.data.ClipboardThemeMode
import com.scott.myclipboard.data.MaxPreviewLineCount
import com.scott.myclipboard.data.MaxWidgetFontSize
import com.scott.myclipboard.data.MinPreviewLineCount
import com.scott.myclipboard.data.MinWidgetFontSize
import com.scott.myclipboard.data.local.ClipboardPhraseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClipboardScreen(
    uiState: ClipboardUiState,
    onSearchQueryChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onDismissEditor: () -> Unit,
    onSavePhrase: () -> Unit,
    onCopyPhrase: (ClipboardPhraseEntity) -> Unit,
    onEditPhrase: (ClipboardPhraseEntity) -> Unit,
    onDeletePhrase: (ClipboardPhraseEntity) -> Unit,
    onShowSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onPreventDuplicatesChange: (Boolean) -> Unit,
    onThemeModeChange: (ClipboardThemeMode) -> Unit,
    onShowCopySuccessMessageChange: (Boolean) -> Unit,
    onCopySuccessMessageTemplateChange: (String) -> Unit,
    onPinFavoritesToTopChange: (Boolean) -> Unit,
    onPreviewLineCountChange: (Int) -> Unit,
    onWidgetFontSizeChange: (Int) -> Unit,
    onSetPhraseFavorite: (ClipboardPhraseEntity, Boolean) -> Unit,
    onDeleteAllPhrases: () -> Unit,
    onSaveClipboardBackup: () -> Unit,
    onRestoreClipboardBackup: () -> Unit,
    onExportClipboardBackup: () -> Unit,
    onImportClipboardBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    Text(stringResource(R.string.search_placeholder))
                },
                singleLine = true,
            )
            TextButton(onClick = onShowSettings) {
                Text(stringResource(R.string.settings_action))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.phrases.isEmpty()) {
            EmptyState(
                searchQuery = uiState.searchQuery,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.phrases,
                    key = { phrase -> phrase.id },
                ) { phrase ->
                    PhraseCard(
                        phrase = phrase,
                        previewLineCount = uiState.previewLineCount,
                        onCopyPhrase = onCopyPhrase,
                        onEditPhrase = onEditPhrase,
                        onDeletePhrase = onDeletePhrase,
                        onSetFavorite = onSetPhraseFavorite,
                    )
                }
            }
        }
    }

    if (uiState.isEditorVisible) {
        PhraseEditorDialog(
            uiState = uiState,
            onTitleChange = onTitleChange,
            onContentChange = onContentChange,
            onDismiss = onDismissEditor,
            onConfirm = onSavePhrase,
        )
    }

    if (uiState.isSettingsVisible) {
        ClipboardSettingsDialog(
            uiState = uiState,
            onDismiss = onDismissSettings,
            onPreventDuplicatesChange = onPreventDuplicatesChange,
            onThemeModeChange = onThemeModeChange,
            onShowCopySuccessMessageChange = onShowCopySuccessMessageChange,
            onCopySuccessMessageTemplateChange = onCopySuccessMessageTemplateChange,
            onPinFavoritesToTopChange = onPinFavoritesToTopChange,
            onPreviewLineCountChange = onPreviewLineCountChange,
            onWidgetFontSizeChange = onWidgetFontSizeChange,
            onDeleteAllPhrases = onDeleteAllPhrases,
            onSaveClipboardBackup = onSaveClipboardBackup,
            onRestoreClipboardBackup = onRestoreClipboardBackup,
            onExportClipboardBackup = onExportClipboardBackup,
            onImportClipboardBackup = onImportClipboardBackup,
        )
    }
}

@Composable
private fun EmptyState(
    searchQuery: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (searchQuery.isBlank()) {
                    stringResource(R.string.empty_title_default)
                } else {
                    stringResource(R.string.empty_title_search)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (searchQuery.isBlank()) {
                    stringResource(R.string.empty_body_default)
                } else {
                    stringResource(R.string.empty_body_search)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhraseCard(
    phrase: ClipboardPhraseEntity,
    previewLineCount: Int,
    onCopyPhrase: (ClipboardPhraseEntity) -> Unit,
    onEditPhrase: (ClipboardPhraseEntity) -> Unit,
    onDeletePhrase: (ClipboardPhraseEntity) -> Unit,
    onSetFavorite: (ClipboardPhraseEntity, Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopyPhrase(phrase) },
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = phrase.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                InlineActionButton(
                    text = if (phrase.isFavorite) {
                        stringResource(R.string.unfavorite_action)
                    } else {
                        stringResource(R.string.favorite_action)
                    },
                    onClick = { onSetFavorite(phrase, !phrase.isFavorite) },
                )
                InlineActionButton(
                    text = stringResource(R.string.edit_action),
                    onClick = { onEditPhrase(phrase) },
                )
                InlineActionButton(
                    text = stringResource(R.string.delete_action),
                    onClick = { onDeletePhrase(phrase) },
                )
            }
            Text(
                text = phrase.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = previewLineCount,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InlineActionButton(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun PhraseEditorDialog(
    uiState: ClipboardUiState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isEditing = uiState.editingPhraseId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) {
                    stringResource(R.string.editor_title_edit)
                } else {
                    stringResource(R.string.editor_title_add)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = uiState.titleInput,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(R.string.title_label))
                    },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.contentInput,
                    onValueChange = onContentChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(R.string.content_label))
                    },
                    minLines = 4,
                )
            }
        },
        confirmButton = {
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
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        },
    )
}

@Composable
private fun ClipboardSettingsDialog(
    uiState: ClipboardUiState,
    onDismiss: () -> Unit,
    onPreventDuplicatesChange: (Boolean) -> Unit,
    onThemeModeChange: (ClipboardThemeMode) -> Unit,
    onShowCopySuccessMessageChange: (Boolean) -> Unit,
    onCopySuccessMessageTemplateChange: (String) -> Unit,
    onPinFavoritesToTopChange: (Boolean) -> Unit,
    onPreviewLineCountChange: (Int) -> Unit,
    onWidgetFontSizeChange: (Int) -> Unit,
    onDeleteAllPhrases: () -> Unit,
    onSaveClipboardBackup: () -> Unit,
    onRestoreClipboardBackup: () -> Unit,
    onExportClipboardBackup: () -> Unit,
    onImportClipboardBackup: () -> Unit,
) {
    var isDeleteConfirmationVisible by remember { mutableStateOf(false) }
    var isRestoreConfirmationVisible by remember { mutableStateOf(false) }
    val backupDateText = remember(uiState.backupSavedAtMillis) {
        uiState.backupSavedAtMillis?.let { savedAtMillis ->
            SimpleDateFormat("MM.dd HH:mm", Locale.getDefault()).format(Date(savedAtMillis))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsSwitchRow(
                    title = stringResource(R.string.copy_success_setting_title),
                    description = stringResource(R.string.copy_success_setting_description),
                    checked = uiState.showCopySuccessMessage,
                    onCheckedChange = onShowCopySuccessMessageChange,
                )
                OutlinedTextField(
                    value = uiState.copySuccessMessageTemplate,
                    onValueChange = onCopySuccessMessageTemplateChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.showCopySuccessMessage,
                    label = {
                        Text(stringResource(R.string.copy_success_message_template_label))
                    },
                    placeholder = {
                        Text(stringResource(R.string.copy_success_message_template_placeholder))
                    },
                    supportingText = {
                        Text(stringResource(R.string.copy_success_message_template_hint))
                    },
                    singleLine = true,
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.prevent_duplicates_setting_title),
                    description = stringResource(R.string.prevent_duplicates_setting_description),
                    checked = uiState.preventDuplicates,
                    onCheckedChange = onPreventDuplicatesChange,
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.pin_favorites_setting_title),
                    description = stringResource(R.string.pin_favorites_setting_description),
                    checked = uiState.pinFavoritesToTop,
                    onCheckedChange = onPinFavoritesToTopChange,
                )
                SettingsStepperRow(
                    title = stringResource(R.string.preview_lines_setting_title),
                    description = stringResource(R.string.preview_lines_setting_description),
                    value = uiState.previewLineCount,
                    minValue = MinPreviewLineCount,
                    maxValue = MaxPreviewLineCount,
                    onValueChange = onPreviewLineCountChange,
                )
                SettingsStepperRow(
                    title = stringResource(R.string.widget_font_size_setting_title),
                    description = stringResource(R.string.widget_font_size_setting_description),
                    value = uiState.widgetFontSize,
                    minValue = MinWidgetFontSize,
                    maxValue = MaxWidgetFontSize,
                    onValueChange = onWidgetFontSizeChange,
                )
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = onSaveClipboardBackup,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.clipboard_backup_save_action))
                        }
                        if (uiState.hasBackup) {
                            TextButton(onClick = { isRestoreConfirmationVisible = true }) {
                                Text(stringResource(R.string.clipboard_backup_restore_short_action))
                            }
                        }
                    }
                    if (backupDateText != null) {
                        Text(
                            text = stringResource(R.string.clipboard_backup_saved_at_label, backupDateText),
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = onExportClipboardBackup,
                            enabled = uiState.hasBackup,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.backup_export_file_action))
                        }
                        TextButton(
                            onClick = onImportClipboardBackup,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.backup_import_file_action))
                        }
                    }
                }
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.theme_setting_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ClipboardThemeMode.entries.forEach { themeMode ->
                    SettingsRadioRow(
                        title = stringResource(themeMode.labelRes),
                        selected = uiState.themeMode == themeMode,
                        onClick = { onThemeModeChange(themeMode) },
                    )
                }
                HorizontalDivider()
                TextButton(onClick = { isDeleteConfirmationVisible = true }) {
                    Text(stringResource(R.string.delete_all_phrases_action))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_action))
            }
        },
    )

    if (isRestoreConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isRestoreConfirmationVisible = false },
            title = {
                Text(stringResource(R.string.clipboard_backup_restore_confirm_title))
            },
            text = {
                Text(stringResource(R.string.clipboard_backup_restore_confirm_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRestoreClipboardBackup()
                        isRestoreConfirmationVisible = false
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.clipboard_backup_restore_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { isRestoreConfirmationVisible = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }

    if (isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteConfirmationVisible = false },
            title = {
                Text(stringResource(R.string.delete_all_confirm_title))
            },
            text = {
                Text(stringResource(R.string.delete_all_confirm_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllPhrases()
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
private fun SettingsSwitchRow(
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
private fun SettingsStepperRow(
    title: String,
    description: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
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
                enabled = value > minValue,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
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
                enabled = value < maxValue,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text("+")
            }
        }
    }
}

@Composable
private fun SettingsRadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private val ClipboardThemeMode.labelRes: Int
    get() = when (this) {
        ClipboardThemeMode.System -> R.string.theme_system_option
        ClipboardThemeMode.Light -> R.string.theme_light_option
        ClipboardThemeMode.Dark -> R.string.theme_dark_option
    }
