package com.soctt.myclipboard.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soctt.myclipboard.R
import com.soctt.myclipboard.data.local.ClipboardPhraseEntity

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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(stringResource(R.string.search_label))
            },
            placeholder = {
                Text(stringResource(R.string.search_placeholder))
            },
            singleLine = true,
        )

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
                        onCopyPhrase = onCopyPhrase,
                        onEditPhrase = onEditPhrase,
                        onDeletePhrase = onDeletePhrase,
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
    onCopyPhrase: (ClipboardPhraseEntity) -> Unit,
    onEditPhrase: (ClipboardPhraseEntity) -> Unit,
    onDeletePhrase: (ClipboardPhraseEntity) -> Unit,
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
                maxLines = 2,
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
