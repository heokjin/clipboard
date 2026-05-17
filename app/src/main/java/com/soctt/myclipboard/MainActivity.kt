package com.soctt.myclipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soctt.myclipboard.R
import com.soctt.myclipboard.data.ClipboardRepository
import com.soctt.myclipboard.data.ClipboardSettingsRepository
import com.soctt.myclipboard.data.ClipboardThemeMode
import com.soctt.myclipboard.data.ReminderRepository
import com.soctt.myclipboard.data.ReminderSettingsRepository
import com.soctt.myclipboard.ui.HomeScreen
import com.soctt.myclipboard.ui.ClipboardViewModel
import com.soctt.myclipboard.ui.ReminderViewModel
import com.soctt.myclipboard.ui.theme.MyclipboardTheme
import com.soctt.myclipboard.widget.MyClipboardWidgetNavigation
import com.soctt.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val WidgetDebugTag = "WidgetDebug"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialPage = when (intent?.getStringExtra(MyClipboardWidgetNavigation.EXTRA_START_PAGE)) {
            MyClipboardWidgetNavigation.PAGE_CLIPBOARD -> 1
            else -> 0
        }
        val initialReminderId = intent?.getLongExtra(MyClipboardWidgetNavigation.EXTRA_REMINDER_ID, -1L)
            ?.takeIf { it >= 0L }
        setContent {
            MyClipboardApp(
                initialPage = initialPage,
                initialReminderId = initialReminderId,
            )
        }
    }
}

@Composable
private fun MyClipboardApp(
    initialPage: Int,
    initialReminderId: Long?,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repository = remember(appContext) {
        ClipboardRepository(appContext)
    }
    val settingsRepository = remember(appContext) {
        ClipboardSettingsRepository(appContext)
    }
    val reminderSettingsRepository = remember(appContext) {
        ReminderSettingsRepository(appContext)
    }
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()
    val useDarkTheme = when (settings.themeMode) {
        ClipboardThemeMode.System -> isSystemInDarkTheme()
        ClipboardThemeMode.Light -> false
        ClipboardThemeMode.Dark -> true
    }

    MyclipboardTheme(darkTheme = useDarkTheme) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val backgroundScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
        val reminderRepository = remember(appContext) {
            ReminderRepository(appContext)
        }
        val snackbarHostState = remember { SnackbarHostState() }
        val clipboardViewModel: ClipboardViewModel = viewModel(
            factory = ClipboardViewModel.factory(
                repository = repository,
                settingsRepository = settingsRepository,
            )
        )
        val reminderViewModel: ReminderViewModel = viewModel(
            factory = ReminderViewModel.factory(
                repository = reminderRepository,
                settingsRepository = reminderSettingsRepository,
            )
        )
        val clipboardUiState by clipboardViewModel.uiState.collectAsStateWithLifecycle()
        val reminderUiState by reminderViewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(clipboardViewModel, settings.showCopySuccessMessage) {
            clipboardViewModel.copySuccessMessages.collect { copiedLabel ->
                if (settings.showCopySuccessMessage) {
                    Toast.makeText(
                        context,
                        settings.copySuccessMessageTemplate.toCopySuccessMessage(
                            copiedLabel = copiedLabel,
                            defaultMessage = context.getString(R.string.copy_success_message, copiedLabel),
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        LaunchedEffect(initialReminderId, reminderViewModel) {
            initialReminderId?.let { reminderId ->
                reminderViewModel.startEditById(reminderId)
            }
        }

        LaunchedEffect(reminderViewModel, snackbarHostState) {
            reminderViewModel.deletedReminderEvents.collect { reminder ->
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.reminder_deleted_message),
                    actionLabel = context.getString(R.string.undo_action),
                    duration = SnackbarDuration.Short,
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    reminderViewModel.restoreReminder(reminder)
                }
            }
        }

        DisposableEffect(
            appContext,
            lifecycleOwner,
            clipboardViewModel,
            reminderViewModel,
        ) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    Log.d(WidgetDebugTag, "MainActivity lifecycle ON_STOP -> persist editors and refresh widget if dirty")
                    backgroundScope.launch {
                        Log.d(WidgetDebugTag, "MainActivity ON_STOP coroutine started")
                        clipboardViewModel.persistEditorIfNeeded()
                        reminderViewModel.persistEditorIfNeeded()
                        MyClipboardWidgetSync.refreshIfDirty(appContext)
                        Log.d(WidgetDebugTag, "MainActivity ON_STOP coroutine finished")
                    }
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        HomeScreen(
            initialPage = initialPage,
            clipboardUiState = clipboardUiState,
            onSearchQueryChange = clipboardViewModel::onSearchQueryChange,
            onShowAddPhraseDialog = clipboardViewModel::showAddDialog,
            onPhraseTitleChange = clipboardViewModel::onTitleChange,
            onPhraseContentChange = clipboardViewModel::onContentChange,
            onDismissPhraseEditor = clipboardViewModel::dismissEditor,
            onSavePhrase = clipboardViewModel::savePhrase,
            onCopyPhrase = { phrase ->
                val clipboardManager = context.getSystemService(ClipboardManager::class.java)
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(phrase.title, phrase.content)
                )
                clipboardViewModel.notifyCopySuccess(phrase)
            },
            onEditPhrase = clipboardViewModel::startEdit,
            onDeletePhrase = clipboardViewModel::deletePhrase,
            onShowClipboardSettings = clipboardViewModel::showSettings,
            onDismissClipboardSettings = clipboardViewModel::dismissSettings,
            onClipboardPreventDuplicatesChange = clipboardViewModel::setPreventDuplicates,
            onClipboardThemeModeChange = clipboardViewModel::setThemeMode,
            onClipboardShowCopySuccessMessageChange = clipboardViewModel::setShowCopySuccessMessage,
            onClipboardCopySuccessMessageTemplateChange = clipboardViewModel::setCopySuccessMessageTemplate,
            onClipboardPinFavoritesToTopChange = clipboardViewModel::setPinFavoritesToTop,
            onClipboardPreviewLineCountChange = clipboardViewModel::setPreviewLineCount,
            onSetPhraseFavorite = clipboardViewModel::setPhraseFavorite,
            onDeleteAllPhrases = clipboardViewModel::deleteAllPhrases,
            reminderUiState = reminderUiState,
            onReminderSearchQueryChange = reminderViewModel::onSearchQueryChange,
            onShowReminderSettings = reminderViewModel::showSettings,
            onDismissReminderSettings = reminderViewModel::dismissSettings,
            onReminderShowWritingHintChange = reminderViewModel::setShowWritingHint,
            onReminderPinImportantToTopChange = reminderViewModel::setPinImportantToTop,
            onReminderPreviewLineCountChange = reminderViewModel::setPreviewLineCount,
            onShowAddReminderDialog = reminderViewModel::showAddDialog,
            onReminderInputChange = reminderViewModel::onReminderInputChange,
            onToggleReminderHighlightSelection = reminderViewModel::toggleHighlightOnSelection,
            onBackFromReminderEditor = reminderViewModel::handleEditorBack,
            onDismissReminderEditor = reminderViewModel::dismissEditor,
            onSaveReminder = reminderViewModel::saveReminder,
            onEditReminder = reminderViewModel::startEdit,
            onDeleteReminder = reminderViewModel::deleteReminder,
            onSetReminderImportant = reminderViewModel::setReminderImportant,
            onDeleteAllReminders = reminderViewModel::deleteAllReminders,
            snackbarHostState = snackbarHostState,
        )
    }
}

private fun String.toCopySuccessMessage(
    copiedLabel: String,
    defaultMessage: String,
): String {
    val template = trim()
    if (template.isBlank()) {
        return defaultMessage
    }
    return template.replace("{title}", copiedLabel)
}
