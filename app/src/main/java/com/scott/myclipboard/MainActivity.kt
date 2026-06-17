package com.scott.myclipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.scott.myclipboard.R
import com.scott.myclipboard.data.ClipboardRepository
import com.scott.myclipboard.data.ClipboardSettingsRepository
import com.scott.myclipboard.data.ClipboardThemeMode
import com.scott.myclipboard.data.ReminderRepository
import com.scott.myclipboard.data.ReminderSettingsRepository
import com.scott.myclipboard.ui.HomeScreen
import com.scott.myclipboard.ui.ClipboardSettingsEvent
import com.scott.myclipboard.ui.ClipboardViewModel
import com.scott.myclipboard.ui.ReminderSettingsEvent
import com.scott.myclipboard.ui.ReminderViewModel
import com.scott.myclipboard.ui.theme.MyclipboardTheme
import com.scott.myclipboard.widget.MyClipboardWidgetNavigation
import com.scott.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val WidgetDebugTag = "WidgetDebug"
private const val BackupMimeType = "application/json"
class MainActivity : ComponentActivity() {
    private var launchRequest by mutableStateOf(AppLaunchRequest())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchRequest = intent.toAppLaunchRequest(sequence = 0)
        setContent {
            MyClipboardApp(launchRequest = launchRequest)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRequest = intent.toAppLaunchRequest(sequence = launchRequest.sequence + 1)
    }
}

@Composable
private fun MyClipboardApp(
    launchRequest: AppLaunchRequest,
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
        val clipboardExportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(BackupMimeType),
        ) { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            val backupJson = clipboardViewModel.getBackupExportJson()
            val message = when {
                backupJson == null -> context.getString(R.string.clipboard_backup_export_unavailable_message)
                context.writeTextToUri(uri, backupJson) -> context.getString(R.string.clipboard_backup_exported_message)
                else -> context.getString(R.string.clipboard_backup_export_failed_message)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        val clipboardImportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            val backupJson = context.readTextFromUri(uri)
            if (backupJson == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.clipboard_backup_import_failed_message),
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                clipboardViewModel.importBackupJson(backupJson)
            }
        }
        val reminderExportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(BackupMimeType),
        ) { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            val backupJson = reminderViewModel.getBackupExportJson()
            val message = when {
                backupJson == null -> context.getString(R.string.reminder_backup_export_unavailable_message)
                context.writeTextToUri(uri, backupJson) -> context.getString(R.string.reminder_backup_exported_message)
                else -> context.getString(R.string.reminder_backup_export_failed_message)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        val reminderImportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }
            val backupJson = context.readTextFromUri(uri)
            if (backupJson == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.reminder_backup_import_failed_message),
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                reminderViewModel.importBackupJson(backupJson)
            }
        }

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

        LaunchedEffect(launchRequest.sequence, launchRequest, clipboardViewModel, reminderViewModel) {
            if (launchRequest.isWidgetNavigation) {
                clipboardViewModel.dismissSettings()
                reminderViewModel.dismissSettings()
                clipboardViewModel.dismissEditor()
                reminderViewModel.dismissEditor()

                when {
                    launchRequest.shouldStartAdd && launchRequest.page == ClipboardPageIndex -> {
                        clipboardViewModel.showAddDialog()
                    }

                    launchRequest.shouldStartAdd -> {
                        reminderViewModel.showAddDialog()
                    }

                    launchRequest.reminderId != null -> {
                        reminderViewModel.startEditById(launchRequest.reminderId)
                    }
                }
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

        LaunchedEffect(reminderViewModel) {
            reminderViewModel.settingsEvents.collect { event ->
                val message = when (event) {
                    is ReminderSettingsEvent.BackupSaved -> {
                        context.getString(R.string.reminder_backup_saved_message, event.count)
                    }

                    is ReminderSettingsEvent.BackupRestored -> {
                        context.getString(R.string.reminder_backup_restored_message, event.count)
                    }

                    is ReminderSettingsEvent.BackupImported -> {
                        context.getString(R.string.reminder_backup_imported_message, event.count)
                    }

                    ReminderSettingsEvent.BackupUnavailable -> {
                        context.getString(R.string.reminder_backup_unavailable_message)
                    }

                    ReminderSettingsEvent.BackupImportFailed -> {
                        context.getString(R.string.reminder_backup_import_failed_message)
                    }
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        LaunchedEffect(clipboardViewModel) {
            clipboardViewModel.settingsEvents.collect { event ->
                val message = when (event) {
                    is ClipboardSettingsEvent.BackupSaved -> {
                        context.getString(R.string.clipboard_backup_saved_message, event.count)
                    }

                    is ClipboardSettingsEvent.BackupRestored -> {
                        context.getString(R.string.clipboard_backup_restored_message, event.count)
                    }

                    is ClipboardSettingsEvent.BackupImported -> {
                        context.getString(R.string.clipboard_backup_imported_message, event.count)
                    }

                    ClipboardSettingsEvent.BackupUnavailable -> {
                        context.getString(R.string.clipboard_backup_unavailable_message)
                    }

                    ClipboardSettingsEvent.BackupImportFailed -> {
                        context.getString(R.string.clipboard_backup_import_failed_message)
                    }
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
            initialPage = launchRequest.page,
            navigationPage = launchRequest.page,
            navigationRequestTick = launchRequest.sequence,
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
            onClipboardWidgetFontSizeChange = clipboardViewModel::setWidgetFontSize,
            onSetPhraseFavorite = clipboardViewModel::setPhraseFavorite,
            onDeleteAllPhrases = clipboardViewModel::deleteAllPhrases,
            onSaveClipboardBackup = clipboardViewModel::saveCurrentBackup,
            onRestoreClipboardBackup = clipboardViewModel::restoreBackup,
            onExportClipboardBackup = {
                clipboardExportLauncher.launch(createBackupFileName("clipboard"))
            },
            onImportClipboardBackup = {
                clipboardImportLauncher.launch(arrayOf(BackupMimeType, "text/*", "application/octet-stream"))
            },
            reminderUiState = reminderUiState,
            onReminderSearchQueryChange = reminderViewModel::onSearchQueryChange,
            onShowReminderSettings = reminderViewModel::showSettings,
            onDismissReminderSettings = reminderViewModel::dismissSettings,
            onReminderShowWritingHintChange = reminderViewModel::setShowWritingHint,
            onReminderPinImportantToTopChange = reminderViewModel::setPinImportantToTop,
            onReminderPreviewLineCountChange = reminderViewModel::setPreviewLineCount,
            onReminderWidgetFontSizeChange = reminderViewModel::setWidgetFontSize,
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
            onSaveReminderBackup = reminderViewModel::saveCurrentBackup,
            onRestoreReminderBackup = reminderViewModel::restoreBackup,
            onExportReminderBackup = {
                reminderExportLauncher.launch(createBackupFileName("reminder"))
            },
            onImportReminderBackup = {
                reminderImportLauncher.launch(arrayOf(BackupMimeType, "text/*", "application/octet-stream"))
            },
            snackbarHostState = snackbarHostState,
        )
    }
}

private const val ReminderPageIndex = 0
private const val ClipboardPageIndex = 1

private data class AppLaunchRequest(
    val sequence: Int = 0,
    val page: Int = ReminderPageIndex,
    val reminderId: Long? = null,
    val shouldStartAdd: Boolean = false,
    val isWidgetNavigation: Boolean = false,
)

private fun Intent?.toAppLaunchRequest(sequence: Int): AppLaunchRequest {
    val startPage = this?.getStringExtra(MyClipboardWidgetNavigation.EXTRA_START_PAGE)
    val page = when (startPage) {
        MyClipboardWidgetNavigation.PAGE_CLIPBOARD -> ClipboardPageIndex
        else -> ReminderPageIndex
    }
    val reminderId = this?.getLongExtra(MyClipboardWidgetNavigation.EXTRA_REMINDER_ID, -1L)
        ?.takeIf { it >= 0L }
    val shouldStartAdd = this?.getStringExtra(MyClipboardWidgetNavigation.EXTRA_START_ACTION) ==
        MyClipboardWidgetNavigation.START_ACTION_ADD
    val isWidgetNavigation = this?.hasExtra(MyClipboardWidgetNavigation.EXTRA_START_PAGE) == true ||
        this?.hasExtra(MyClipboardWidgetNavigation.EXTRA_START_ACTION) == true ||
        this?.hasExtra(MyClipboardWidgetNavigation.EXTRA_REMINDER_ID) == true

    return AppLaunchRequest(
        sequence = sequence,
        page = page,
        reminderId = reminderId,
        shouldStartAdd = shouldStartAdd,
        isWidgetNavigation = isWidgetNavigation,
    )
}

private fun createBackupFileName(kind: String): String {
    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return "my-clipboard-$kind-backup-$timestamp.json"
}

private fun Context.writeTextToUri(uri: Uri, text: String): Boolean {
    return runCatching {
        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(text)
        } ?: error("Could not open output stream")
    }.isSuccess
}

private fun Context.readTextFromUri(uri: Uri): String? {
    return runCatching {
        contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.readText()
        }
    }.getOrNull()
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
