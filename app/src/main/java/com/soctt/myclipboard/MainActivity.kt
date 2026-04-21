package com.soctt.myclipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soctt.myclipboard.R
import com.soctt.myclipboard.data.ClipboardRepository
import com.soctt.myclipboard.data.ReminderRepository
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
            MyClipboardWidgetNavigation.PAGE_REMINDER -> 1
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
    MyclipboardTheme {
        val context = LocalContext.current
        val appContext = context.applicationContext
        val lifecycleOwner = LocalLifecycleOwner.current
        val backgroundScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
        val repository = remember(appContext) {
            ClipboardRepository(appContext)
        }
        val reminderRepository = remember(appContext) {
            ReminderRepository(appContext)
        }
        val clipboardViewModel: ClipboardViewModel = viewModel(
            factory = ClipboardViewModel.factory(repository)
        )
        val reminderViewModel: ReminderViewModel = viewModel(
            factory = ReminderViewModel.factory(reminderRepository)
        )
        val clipboardUiState by clipboardViewModel.uiState.collectAsStateWithLifecycle()
        val reminderUiState by reminderViewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(clipboardViewModel) {
            clipboardViewModel.copySuccessMessages.collect { copiedLabel ->
                Toast.makeText(
                    context,
                    context.getString(R.string.copy_success_message, copiedLabel),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        LaunchedEffect(initialReminderId, reminderViewModel) {
            initialReminderId?.let { reminderId ->
                reminderViewModel.startEditById(reminderId)
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
            reminderUiState = reminderUiState,
            onShowAddReminderDialog = reminderViewModel::showAddDialog,
            onReminderInputChange = reminderViewModel::onReminderInputChange,
            onBackFromReminderEditor = reminderViewModel::handleEditorBack,
            onDismissReminderEditor = reminderViewModel::dismissEditor,
            onSaveReminder = reminderViewModel::saveReminder,
            onEditReminder = reminderViewModel::startEdit,
            onDeleteReminder = reminderViewModel::deleteReminder,
        )
    }
}
