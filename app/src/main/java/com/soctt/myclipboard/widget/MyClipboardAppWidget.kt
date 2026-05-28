package com.soctt.myclipboard.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.soctt.myclipboard.MainActivity
import com.soctt.myclipboard.R
import com.soctt.myclipboard.data.ClipboardRepository
import com.soctt.myclipboard.data.ReminderRepository
import com.soctt.myclipboard.data.local.ClipboardPhraseEntity
import com.soctt.myclipboard.data.local.ReminderEntity
import com.soctt.myclipboard.reminder.buildReminderWidgetText
import com.soctt.myclipboard.reminder.styleSpans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val WidgetDebugTag = "WidgetDebug"
private const val WidgetReminderDataLimit = 40
private const val WidgetClipboardDataLimit = 24

class MyClipboardAppWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: androidx.glance.GlanceId,
    ) {
        val reminderRepository = ReminderRepository(context)
        val clipboardRepository = ClipboardRepository(context)
        val snapshot = withContext(Dispatchers.IO) {
            WidgetSnapshot(
                reminders = reminderRepository.getRecentReminders(limit = WidgetReminderDataLimit),
                phrases = clipboardRepository.getRecentPhrases(limit = WidgetClipboardDataLimit),
            )
        }
        Log.d(
            WidgetDebugTag,
            "MyClipboardAppWidget.provideGlance reminders=${snapshot.reminders.size} phrases=${snapshot.phrases.size} glanceId=$id"
        )

        provideContent {
            val reminders by reminderRepository.observeRecentReminders(limit = WidgetReminderDataLimit)
                .collectAsState(initial = snapshot.reminders)
            val phrases by clipboardRepository.observeRecentPhrases(limit = WidgetClipboardDataLimit)
                .collectAsState(initial = snapshot.phrases)

            LaunchedEffect(reminders, phrases) {
                Log.d(
                    WidgetDebugTag,
                    "MyClipboardAppWidget.WidgetContent synced reminders=${reminders.size} phrases=${phrases.size}"
                )
                MyClipboardWidgetSync.clearDirty("widget_content_synced")
            }

            WidgetContent(
                reminders = reminders,
                phrases = phrases,
            )
        }
    }
}

class MyClipboardAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyClipboardAppWidget()
}

private data class WidgetSnapshot(
    val reminders: List<ReminderEntity>,
    val phrases: List<ClipboardPhraseEntity>,
)

private enum class WidgetPage(
    val value: String,
) {
    Reminder(MyClipboardWidgetNavigation.PAGE_REMINDER),
    Clipboard(MyClipboardWidgetNavigation.PAGE_CLIPBOARD),
    ;

    companion object {
        fun fromValue(value: String?): WidgetPage {
            return entries.firstOrNull { it.value == value } ?: Reminder
        }
    }
}

private object WidgetStateKeys {
    val currentPage = stringPreferencesKey("widget_current_page")
}

private object WidgetActionKeys {
    val page = ActionParameters.Key<String>("widget_page")
    val phraseId = ActionParameters.Key<Long>("widget_phrase_id")
    val startPage = ActionParameters.Key<String>(MyClipboardWidgetNavigation.EXTRA_START_PAGE)
    val startAction = ActionParameters.Key<String>(MyClipboardWidgetNavigation.EXTRA_START_ACTION)
    val reminderId = ActionParameters.Key<Long>(MyClipboardWidgetNavigation.EXTRA_REMINDER_ID)
}

private object WidgetColors {
    val background = Color(0xFFF6F2EA)
    val surface = Color(0xFFFFFFFF)
    val selectedTab = Color(0xFFE5D4B8)
    val unselectedTab = Color(0xFFF1EBE2)
    val itemSurface = Color(0xFFFCF9F4)
    val subtleText = Color(0xFF6F675D)
}

@androidx.compose.runtime.Composable
private fun WidgetContent(
    reminders: List<ReminderEntity>,
    phrases: List<ClipboardPhraseEntity>,
) {
    val preferences = currentState<Preferences>()
    val currentPage = WidgetPage.fromValue(preferences[WidgetStateKeys.currentPage])
    val size = LocalSize.current
    val isWide = size.width.value >= 300f
    val reminderRows = reminderRowCount(
        heightDp = size.height.value,
        columns = if (isWide) 2 else 1,
    )
    val clipboardRows = clipboardRowCount(
        heightDp = size.height.value,
        columns = if (isWide) 2 else 1,
    )
    Log.d(
        WidgetDebugTag,
        "WidgetContent size width=${size.width.value} height=${size.height.value} page=${currentPage.value} reminders=${reminders.size} phrases=${phrases.size} reminderRows=$reminderRows clipboardRows=$clipboardRows"
    )
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            Row {
                WidgetTabChip(
                    text = when (currentPage) {
                        WidgetPage.Reminder -> "● " + currentPageLabel(WidgetPage.Reminder)
                        WidgetPage.Clipboard -> currentPageLabel(WidgetPage.Reminder)
                    },
                    selected = currentPage == WidgetPage.Reminder,
                    action = actionRunCallback<SwitchWidgetPageAction>(
                        actionParametersOf(WidgetActionKeys.page to WidgetPage.Reminder.value)
                    ),
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                WidgetTabChip(
                    text = when (currentPage) {
                        WidgetPage.Clipboard -> "● " + currentPageLabel(WidgetPage.Clipboard)
                        WidgetPage.Reminder -> currentPageLabel(WidgetPage.Clipboard)
                    },
                    selected = currentPage == WidgetPage.Clipboard,
                    action = actionRunCallback<SwitchWidgetPageAction>(
                        actionParametersOf(WidgetActionKeys.page to WidgetPage.Clipboard.value)
                    ),
                )
            }

            Text(
                text = "",
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(
                                WidgetActionKeys.startPage to currentPage.value,
                            )
                        )
                    )
                    .padding(vertical = 12.dp),
                maxLines = 1,
            )

            Text(
                text = "+",
                modifier = GlanceModifier
                    .background(WidgetColors.selectedTab)
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(
                                WidgetActionKeys.startPage to currentPage.value,
                                WidgetActionKeys.startAction to MyClipboardWidgetNavigation.START_ACTION_ADD,
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
        ) {
            when (currentPage) {
                WidgetPage.Reminder -> ReminderWidgetList(
                    reminders = reminders,
                    columns = if (isWide) 2 else 1,
                )

                WidgetPage.Clipboard -> ClipboardWidgetList(
                    phrases = phrases,
                    columns = if (isWide) 2 else 1,
                    isWide = isWide,
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun currentPageLabel(
    page: WidgetPage,
): String {
    val context = androidx.glance.LocalContext.current
    return when (page) {
        WidgetPage.Reminder -> context.getString(R.string.widget_tab_reminder)
        WidgetPage.Clipboard -> context.getString(R.string.widget_tab_clipboard)
    }
}

@androidx.compose.runtime.Composable
private fun WidgetTabChip(
    text: String,
    selected: Boolean,
    action: Action,
) {
    Text(
        text = text,
        modifier = GlanceModifier
            .background(if (selected) WidgetColors.selectedTab else WidgetColors.unselectedTab)
            .clickable(action)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        ),
        maxLines = 1,
    )
}

@androidx.compose.runtime.Composable
private fun ReminderWidgetList(
    reminders: List<ReminderEntity>,
    columns: Int,
) {
    val context = androidx.glance.LocalContext.current

    if (reminders.isEmpty()) {
        EmptyWidgetMessage(
            text = context.getString(R.string.widget_empty_reminders),
            action = actionStartActivity<MainActivity>(
                actionParametersOf(WidgetActionKeys.startPage to MyClipboardWidgetNavigation.PAGE_REMINDER)
            ),
        )
        return
    }

    val rows = reminders.chunked(columns)
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(
            items = rows,
            itemId = { rowItems -> reminderRowStableId(rowItems) },
        ) { rowItems ->
            ReminderWidgetRow(
                rowItems = rowItems,
                columns = columns,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun ReminderWidgetRow(
    rowItems: List<ReminderEntity>,
    columns: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(modifier = modifier) {
        rowItems.forEachIndexed { itemIndex, reminder ->
            ReminderWidgetItem(
                reminder = reminder,
                modifier = if (columns > 1 && rowItems.size == columns) {
                    GlanceModifier.defaultWeight().fillMaxHeight()
                } else {
                    GlanceModifier.fillMaxWidth()
                },
            )
            if (columns > 1 && itemIndex != rowItems.lastIndex) {
                Spacer(modifier = GlanceModifier.width(6.dp))
            }
        }
        if (columns > 1 && rowItems.size < columns) {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }
}

@androidx.compose.runtime.Composable
private fun ReminderWidgetItem(
    reminder: ReminderEntity,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = buildReminderWidgetText(
            text = reminder.text,
            spans = reminder.styleSpans(),
        ),
        modifier = modifier
            .background(WidgetColors.itemSurface)
            .clickable(
                actionStartActivity<MainActivity>(
                    actionParametersOf(
                        WidgetActionKeys.startPage to MyClipboardWidgetNavigation.PAGE_REMINDER,
                        WidgetActionKeys.reminderId to reminder.id,
                    )
                )
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        style = TextStyle(fontSize = 13.sp),
        maxLines = 2,
    )
}

@androidx.compose.runtime.Composable
private fun ClipboardWidgetList(
    phrases: List<ClipboardPhraseEntity>,
    columns: Int,
    isWide: Boolean,
) {
    val context = androidx.glance.LocalContext.current

    if (phrases.isEmpty()) {
        EmptyWidgetMessage(
            text = context.getString(R.string.widget_empty_clipboard),
            action = actionStartActivity<MainActivity>(
                actionParametersOf(WidgetActionKeys.startPage to MyClipboardWidgetNavigation.PAGE_CLIPBOARD)
            ),
        )
        return
    }

    val rows = phrases.chunked(columns)
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(
            items = rows,
            itemId = { rowItems -> clipboardRowStableId(rowItems) },
        ) { rowItems ->
            ClipboardWidgetRow(
                rowItems = rowItems,
                columns = columns,
                isWide = isWide,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            )
        }
    }
}

private fun reminderRowStableId(
    rowItems: List<ReminderEntity>,
): Long {
    return rowItems.fold(17L) { acc, item ->
        (acc * 31L) + item.id
    }
}

private fun clipboardRowStableId(
    rowItems: List<ClipboardPhraseEntity>,
): Long {
    return rowItems.fold(17L) { acc, item ->
        (acc * 31L) + item.id
    }
}

@androidx.compose.runtime.Composable
private fun ClipboardWidgetRow(
    rowItems: List<ClipboardPhraseEntity>,
    columns: Int,
    isWide: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(modifier = modifier) {
        rowItems.forEachIndexed { itemIndex, phrase ->
            ClipboardWidgetItem(
                phrase = phrase,
                modifier = if (columns > 1 && rowItems.size == columns) {
                    GlanceModifier.defaultWeight().fillMaxHeight()
                } else {
                    GlanceModifier.fillMaxWidth()
                },
                contentMaxLines = if (isWide) 2 else 1,
            )
            if (columns > 1 && itemIndex != rowItems.lastIndex) {
                Spacer(modifier = GlanceModifier.width(6.dp))
            }
        }
        if (columns > 1 && rowItems.size < columns) {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }
}

private fun reminderRowCount(
    heightDp: Float,
    columns: Int,
): Int {
    val reservedHeight = 78f
    val rowHeight = if (columns > 1) 60f else 66f
    val rowGap = 6f
    val availableHeight = (heightDp - reservedHeight).coerceAtLeast(rowHeight)
    return (((availableHeight + rowGap) / (rowHeight + rowGap)).toInt()).coerceIn(2, 12)
}

private fun clipboardRowCount(
    heightDp: Float,
    columns: Int,
): Int {
    val reservedHeight = 78f
    val rowHeight = if (columns > 1) 62f else 70f
    val rowGap = 6f
    val availableHeight = (heightDp - reservedHeight).coerceAtLeast(rowHeight)
    return (((availableHeight + rowGap) / (rowHeight + rowGap)).toInt()).coerceIn(2, 8)
}

@androidx.compose.runtime.Composable
private fun ClipboardWidgetItem(
    phrase: ClipboardPhraseEntity,
    modifier: GlanceModifier = GlanceModifier,
    contentMaxLines: Int,
) {
    val combinedText = buildString {
        append(phrase.title)
        if (phrase.content.isNotBlank()) {
            append("\n")
            append(phrase.content)
        }
    }

    Text(
        text = combinedText,
        modifier = modifier
            .background(WidgetColors.itemSurface)
            .clickable(
                actionRunCallback<CopyPhraseToClipboardAction>(
                    actionParametersOf(WidgetActionKeys.phraseId to phrase.id)
                )
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        ),
        maxLines = contentMaxLines + 1,
    )
}

@androidx.compose.runtime.Composable
private fun EmptyWidgetMessage(
    text: String,
    action: Action,
) {
    Text(
        text = text,
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetColors.surface)
            .clickable(action)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        style = TextStyle(
            fontSize = 12.sp,
            color = ColorProvider(WidgetColors.subtleText),
        ),
        maxLines = 3,
    )
}

class SwitchWidgetPageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters,
    ) {
        val page = parameters[WidgetActionKeys.page] ?: WidgetPage.Reminder.value
        Log.d(WidgetDebugTag, "SwitchWidgetPageAction page=$page")
        updateAppWidgetState(context, glanceId) { preferences ->
            preferences[WidgetStateKeys.currentPage] = page
        }
        MyClipboardAppWidget().update(context, glanceId)
    }
}

class CopyPhraseToClipboardAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters,
    ) {
        val phraseId = parameters[WidgetActionKeys.phraseId] ?: return
        Log.d(WidgetDebugTag, "CopyPhraseToClipboardAction phraseId=$phraseId")
        val phrase = withContext(Dispatchers.IO) {
            ClipboardRepository(context).getPhraseById(phraseId)
        } ?: return

        val clipboardManager = context.getSystemService(ClipboardManager::class.java)
        clipboardManager.setPrimaryClip(
            ClipData.newPlainText(phrase.title, phrase.content)
        )

        val label = phrase.title.ifBlank {
            phrase.content.lineSequence().firstOrNull().orEmpty().take(24)
        }
        withContext(Dispatchers.Main.immediate) {
            if (Looper.myLooper() != null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.copy_success_message, label),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        MyClipboardAppWidget().update(context, glanceId)
    }
}
