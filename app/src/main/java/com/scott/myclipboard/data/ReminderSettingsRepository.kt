package com.scott.myclipboard.data

import android.content.Context
import android.content.SharedPreferences
import com.scott.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val ReminderSettingsPrefsName = "reminder_settings"
private const val ShowWritingHintKey = "show_writing_hint"
private const val PinImportantToTopKey = "pin_important_to_top"
private const val PreviewLineCountKey = "preview_line_count"
private const val WidgetFontSizeKey = "widget_font_size"

const val MinReminderPreviewLineCount = 1
const val MaxReminderPreviewLineCount = 5
const val MinReminderWidgetFontSize = 10
const val MaxReminderWidgetFontSize = 18

data class ReminderSettings(
    val showWritingHint: Boolean = true,
    val pinImportantToTop: Boolean = true,
    val previewLineCount: Int = 2,
    val widgetFontSize: Int = 13,
)

class ReminderSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        ReminderSettingsPrefsName,
        Context.MODE_PRIVATE,
    )
    private val _settings = MutableStateFlow(prefs.readSettings())

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, _ ->
        _settings.value = sharedPrefs.readSettings()
    }

    val settings: StateFlow<ReminderSettings> = _settings.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setShowWritingHint(enabled: Boolean) {
        prefs.edit()
            .putBoolean(ShowWritingHintKey, enabled)
            .apply()
    }

    fun setPinImportantToTop(enabled: Boolean) {
        prefs.edit()
            .putBoolean(PinImportantToTopKey, enabled)
            .apply()
    }

    fun setPreviewLineCount(lineCount: Int) {
        prefs.edit()
            .putInt(PreviewLineCountKey, lineCount.coerceIn(MinReminderPreviewLineCount, MaxReminderPreviewLineCount))
            .apply()
    }

    fun setWidgetFontSize(fontSize: Int) {
        prefs.edit()
            .putInt(WidgetFontSizeKey, fontSize.coerceIn(MinReminderWidgetFontSize, MaxReminderWidgetFontSize))
            .apply()
        MyClipboardWidgetSync.markDirty()
    }

    private fun SharedPreferences.readSettings(): ReminderSettings {
        return ReminderSettings(
            showWritingHint = getBoolean(ShowWritingHintKey, true),
            pinImportantToTop = getBoolean(PinImportantToTopKey, true),
            previewLineCount = getInt(PreviewLineCountKey, 2)
                .coerceIn(MinReminderPreviewLineCount, MaxReminderPreviewLineCount),
            widgetFontSize = getInt(WidgetFontSizeKey, 13)
                .coerceIn(MinReminderWidgetFontSize, MaxReminderWidgetFontSize),
        )
    }
}
