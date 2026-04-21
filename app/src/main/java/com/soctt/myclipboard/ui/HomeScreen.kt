package com.soctt.myclipboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.soctt.myclipboard.R
import com.soctt.myclipboard.data.local.ClipboardPhraseEntity
import com.soctt.myclipboard.data.local.ReminderEntity
import kotlinx.coroutines.launch

private enum class HomePage(
    val titleRes: Int,
) {
    Clipboard(R.string.clipboard_tab_title),
    Reminder(R.string.reminder_tab_title),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialPage: Int = 0,
    clipboardUiState: ClipboardUiState,
    onSearchQueryChange: (String) -> Unit,
    onShowAddPhraseDialog: () -> Unit,
    onPhraseTitleChange: (String) -> Unit,
    onPhraseContentChange: (String) -> Unit,
    onDismissPhraseEditor: () -> Unit,
    onSavePhrase: () -> Unit,
    onCopyPhrase: (ClipboardPhraseEntity) -> Unit,
    onEditPhrase: (ClipboardPhraseEntity) -> Unit,
    onDeletePhrase: (ClipboardPhraseEntity) -> Unit,
    reminderUiState: ReminderUiState,
    onShowAddReminderDialog: () -> Unit,
    onReminderInputChange: (String) -> Unit,
    onBackFromReminderEditor: () -> Unit,
    onDismissReminderEditor: () -> Unit,
    onSaveReminder: () -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = HomePage.entries
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pages.lastIndex),
        pageCount = { pages.size },
    )
    val coroutineScope = rememberCoroutineScope()
    val currentPage = pages[pagerState.currentPage]
    val isEditorVisible = when (currentPage) {
        HomePage.Clipboard -> clipboardUiState.isEditorVisible
        HomePage.Reminder -> reminderUiState.isEditorVisible
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
            )
        },
        floatingActionButton = {
            if (!isEditorVisible) {
                FloatingActionButton(
                    onClick = {
                        when (currentPage) {
                            HomePage.Clipboard -> onShowAddPhraseDialog()
                            HomePage.Reminder -> onShowAddReminderDialog()
                        }
                    },
                ) {
                    Text(stringResource(R.string.add_action))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (!isEditorVisible) {
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    pages.forEachIndexed { index, page ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(stringResource(page.titleRes))
                            },
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (pages[page]) {
                    HomePage.Clipboard -> {
                        ClipboardScreen(
                            uiState = clipboardUiState,
                            onSearchQueryChange = onSearchQueryChange,
                            onTitleChange = onPhraseTitleChange,
                            onContentChange = onPhraseContentChange,
                            onDismissEditor = onDismissPhraseEditor,
                            onSavePhrase = onSavePhrase,
                            onCopyPhrase = onCopyPhrase,
                            onEditPhrase = onEditPhrase,
                            onDeletePhrase = onDeletePhrase,
                        )
                    }

                    HomePage.Reminder -> {
                        ReminderScreen(
                            uiState = reminderUiState,
                            onReminderInputChange = onReminderInputChange,
                            onBackFromEditor = onBackFromReminderEditor,
                            onDismissEditor = onDismissReminderEditor,
                            onSaveReminder = onSaveReminder,
                            onEditReminder = onEditReminder,
                            onDeleteReminder = onDeleteReminder,
                        )
                    }
                }
            }
        }
    }
}
