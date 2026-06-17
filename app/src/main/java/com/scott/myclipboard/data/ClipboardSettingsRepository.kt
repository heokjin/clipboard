package com.scott.myclipboard.data

import android.content.Context
import android.content.SharedPreferences
import com.scott.myclipboard.widget.MyClipboardWidgetSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val ClipboardSettingsPrefsName = "clipboard_settings"
private const val PreventDuplicatesKey = "prevent_duplicates"
private const val ThemeModeKey = "theme_mode"
private const val ShowCopySuccessMessageKey = "show_copy_success_message"
private const val CopySuccessMessageTemplateKey = "copy_success_message_template"
private const val PinFavoritesToTopKey = "pin_favorites_to_top"
private const val PreviewLineCountKey = "preview_line_count"
private const val WidgetFontSizeKey = "widget_font_size"

const val MinPreviewLineCount = 1
const val MaxPreviewLineCount = 5
const val MinWidgetFontSize = 10
const val MaxWidgetFontSize = 18

enum class ClipboardThemeMode {
    System,
    Light,
    Dark,
}

data class ClipboardSettings(
    val preventDuplicates: Boolean = true,
    val themeMode: ClipboardThemeMode = ClipboardThemeMode.System,
    val showCopySuccessMessage: Boolean = true,
    val copySuccessMessageTemplate: String = "",
    val pinFavoritesToTop: Boolean = true,
    val previewLineCount: Int = 2,
    val widgetFontSize: Int = 12,
)

class ClipboardSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        ClipboardSettingsPrefsName,
        Context.MODE_PRIVATE,
    )
    private val _settings = MutableStateFlow(prefs.readSettings())

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, _ ->
        _settings.value = sharedPrefs.readSettings()
    }

    val settings: StateFlow<ClipboardSettings> = _settings.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setPreventDuplicates(enabled: Boolean) {
        prefs.edit()
            .putBoolean(PreventDuplicatesKey, enabled)
            .apply()
    }

    fun setThemeMode(themeMode: ClipboardThemeMode) {
        prefs.edit()
            .putString(ThemeModeKey, themeMode.name)
            .apply()
    }

    fun setShowCopySuccessMessage(enabled: Boolean) {
        prefs.edit()
            .putBoolean(ShowCopySuccessMessageKey, enabled)
            .apply()
    }

    fun setCopySuccessMessageTemplate(template: String) {
        prefs.edit()
            .putString(CopySuccessMessageTemplateKey, template)
            .apply()
    }

    fun setPinFavoritesToTop(enabled: Boolean) {
        prefs.edit()
            .putBoolean(PinFavoritesToTopKey, enabled)
            .apply()
    }

    fun setPreviewLineCount(lineCount: Int) {
        prefs.edit()
            .putInt(PreviewLineCountKey, lineCount.coerceIn(MinPreviewLineCount, MaxPreviewLineCount))
            .apply()
    }

    fun setWidgetFontSize(fontSize: Int) {
        prefs.edit()
            .putInt(WidgetFontSizeKey, fontSize.coerceIn(MinWidgetFontSize, MaxWidgetFontSize))
            .apply()
        MyClipboardWidgetSync.markDirty()
    }

    private fun SharedPreferences.readSettings(): ClipboardSettings {
        return ClipboardSettings(
            preventDuplicates = getBoolean(PreventDuplicatesKey, true),
            themeMode = getString(ThemeModeKey, null)
                ?.let { storedValue ->
                    ClipboardThemeMode.entries.firstOrNull { it.name == storedValue }
                }
                ?: ClipboardThemeMode.System,
            showCopySuccessMessage = getBoolean(ShowCopySuccessMessageKey, true),
            copySuccessMessageTemplate = getString(CopySuccessMessageTemplateKey, null).orEmpty(),
            pinFavoritesToTop = getBoolean(PinFavoritesToTopKey, true),
            previewLineCount = getInt(PreviewLineCountKey, 2)
                .coerceIn(MinPreviewLineCount, MaxPreviewLineCount),
            widgetFontSize = getInt(WidgetFontSizeKey, 12)
                .coerceIn(MinWidgetFontSize, MaxWidgetFontSize),
        )
    }
}
