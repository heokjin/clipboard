package com.soctt.myclipboard.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.soctt.myclipboard.R
import com.soctt.myclipboard.data.ClipboardThemeMode
import com.soctt.myclipboard.data.local.ClipboardPhraseEntity
import com.soctt.myclipboard.data.local.ReminderEntity
import kotlinx.coroutines.launch

private const val WidgetDebugTag = "WidgetDebug"

private enum class HomePage(
    val titleRes: Int,
) {
    Reminder(R.string.reminder_tab_title),
    Clipboard(R.string.clipboard_tab_title),
    Game(R.string.game_tab_title),
}

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
    onShowClipboardSettings: () -> Unit,
    onDismissClipboardSettings: () -> Unit,
    onClipboardPreventDuplicatesChange: (Boolean) -> Unit,
    onClipboardThemeModeChange: (ClipboardThemeMode) -> Unit,
    onClipboardShowCopySuccessMessageChange: (Boolean) -> Unit,
    onClipboardCopySuccessMessageTemplateChange: (String) -> Unit,
    onClipboardPinFavoritesToTopChange: (Boolean) -> Unit,
    onClipboardPreviewLineCountChange: (Int) -> Unit,
    onSetPhraseFavorite: (ClipboardPhraseEntity, Boolean) -> Unit,
    onDeleteAllPhrases: () -> Unit,
    reminderUiState: ReminderUiState,
    onReminderSearchQueryChange: (String) -> Unit,
    onShowReminderSettings: () -> Unit,
    onDismissReminderSettings: () -> Unit,
    onReminderShowWritingHintChange: (Boolean) -> Unit,
    onReminderPinImportantToTopChange: (Boolean) -> Unit,
    onReminderPreviewLineCountChange: (Int) -> Unit,
    onShowAddReminderDialog: () -> Unit,
    onReminderInputChange: (TextFieldValue) -> Unit,
    onToggleReminderHighlightSelection: () -> Unit,
    onBackFromReminderEditor: () -> Unit,
    onDismissReminderEditor: () -> Unit,
    onSaveReminder: () -> Unit,
    onEditReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onSetReminderImportant: (ReminderEntity, Boolean) -> Unit,
    onDeleteAllReminders: () -> Unit,
    snackbarHostState: SnackbarHostState,
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
        HomePage.Game -> false
    }
    val isSettingsVisible = when (currentPage) {
        HomePage.Clipboard -> clipboardUiState.isSettingsVisible
        HomePage.Reminder -> reminderUiState.isSettingsVisible
        HomePage.Game -> false
    }
    val isPagerSwipeEnabled = !isEditorVisible && currentPage != HomePage.Game

    BackHandler(enabled = currentPage == HomePage.Reminder && reminderUiState.isEditorVisible) {
        Log.d(WidgetDebugTag, "HomeScreen.BackHandler -> onBackFromReminderEditor")
        onBackFromReminderEditor()
    }

    BackHandler(
        enabled = currentPage != HomePage.Reminder && !isEditorVisible && !isSettingsVisible,
    ) {
        Log.d(WidgetDebugTag, "HomeScreen.BackHandler -> return to reminder tab from ${currentPage.name}")
        coroutineScope.launch {
            pagerState.animateScrollToPage(HomePage.Reminder.ordinal)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            if (!isEditorVisible && currentPage != HomePage.Game) {
                FloatingActionButton(
                    onClick = {
                        when (currentPage) {
                            HomePage.Clipboard -> onShowAddPhraseDialog()
                            HomePage.Reminder -> onShowAddReminderDialog()
                            HomePage.Game -> Unit
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
                userScrollEnabled = isPagerSwipeEnabled,
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
                            onShowSettings = onShowClipboardSettings,
                            onDismissSettings = onDismissClipboardSettings,
                            onPreventDuplicatesChange = onClipboardPreventDuplicatesChange,
                            onThemeModeChange = onClipboardThemeModeChange,
                            onShowCopySuccessMessageChange = onClipboardShowCopySuccessMessageChange,
                            onCopySuccessMessageTemplateChange = onClipboardCopySuccessMessageTemplateChange,
                            onPinFavoritesToTopChange = onClipboardPinFavoritesToTopChange,
                            onPreviewLineCountChange = onClipboardPreviewLineCountChange,
                            onSetPhraseFavorite = onSetPhraseFavorite,
                            onDeleteAllPhrases = onDeleteAllPhrases,
                        )
                    }

                    HomePage.Reminder -> {
                        ReminderScreen(
                            uiState = reminderUiState,
                            onSearchQueryChange = onReminderSearchQueryChange,
                            onShowSettings = onShowReminderSettings,
                            onDismissSettings = onDismissReminderSettings,
                            onShowWritingHintChange = onReminderShowWritingHintChange,
                            onPinImportantToTopChange = onReminderPinImportantToTopChange,
                            onPreviewLineCountChange = onReminderPreviewLineCountChange,
                            onReminderInputChange = onReminderInputChange,
                            onToggleHighlightSelection = onToggleReminderHighlightSelection,
                            onDismissEditor = onDismissReminderEditor,
                            onSaveReminder = onSaveReminder,
                            onEditReminder = onEditReminder,
                            onDeleteReminder = onDeleteReminder,
                            onSetReminderImportant = onSetReminderImportant,
                            onDeleteAllReminders = onDeleteAllReminders,
                        )
                    }

                    HomePage.Game -> {
                        PlatformGameScreen()
                    }
                }
            }
        }
    }
}
