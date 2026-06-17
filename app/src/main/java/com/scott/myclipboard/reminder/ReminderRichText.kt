package com.scott.myclipboard.reminder

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import com.scott.myclipboard.data.local.ReminderEntity
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

data class ReminderStyleSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val colorHex: String? = null,
)

data class ReminderTextStyle(
    val bold: Boolean = false,
    val colorHex: String? = null,
)

data class ReminderSelectionStyle(
    val hasSelection: Boolean,
    val highlighted: Boolean,
)

private val DefaultReminderTextStyle = ReminderTextStyle()
private const val EmptyStyleJson = "[]"
const val ReminderHighlightRedHex = "#D9485F"

fun ReminderEntity.styleSpans(): List<ReminderStyleSpan> {
    return decodeReminderStyleSpans(styleSpansJson, text.length)
}

fun encodeReminderStyleSpans(spans: List<ReminderStyleSpan>): String {
    if (spans.isEmpty()) return EmptyStyleJson
    val array = JSONArray()
    spans.forEach { span ->
        array.put(
            JSONObject().apply {
                put("start", span.start)
                put("end", span.end)
                put("bold", span.bold)
                put("colorHex", span.colorHex)
            }
        )
    }
    return array.toString()
}

fun decodeReminderStyleSpans(
    json: String?,
    textLength: Int,
): List<ReminderStyleSpan> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                val start = item.optInt("start", 0)
                val end = item.optInt("end", 0)
                val bold = item.optBoolean("bold", false)
                val colorHex = item.optString("colorHex").takeIf { it.isNotBlank() }
                val normalized = ReminderStyleSpan(
                    start = start,
                    end = end,
                    bold = bold,
                    colorHex = colorHex,
                ).normalize(textLength)
                if (normalized != null) {
                    add(normalized)
                }
            }
        }
    }.getOrDefault(emptyList())
}

fun buildReminderAnnotatedString(
    text: String,
    spans: List<ReminderStyleSpan>,
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    val normalized = normalizeReminderStyleSpans(text, spans)
    return buildAnnotatedString {
        append(text)
        normalized.forEach { span ->
            addStyle(
                SpanStyle(
                    color = parseReminderColorOrNull(span.colorHex ?: ReminderHighlightRedHex) ?: Color.Unspecified,
                ),
                start = span.start,
                end = span.end,
            )
        }
    }
}

fun buildReminderWidgetText(
    text: String,
    spans: List<ReminderStyleSpan>,
): String {
    if (text.isEmpty()) return ""
    val normalized = normalizeReminderStyleSpans(text, spans)
    if (normalized.isEmpty()) return text

    val builder = StringBuilder(text.length + normalized.size * 2)
    var cursor = 0
    normalized.forEach { span ->
        if (cursor < span.start) {
            builder.append(text.substring(cursor, span.start))
        }
        builder.append('【')
        builder.append(text.substring(span.start, span.end))
        builder.append('】')
        cursor = span.end
    }
    if (cursor < text.length) {
        builder.append(text.substring(cursor))
    }
    return builder.toString()
}

fun normalizeReminderStyleSpans(
    text: String,
    spans: List<ReminderStyleSpan>,
): List<ReminderStyleSpan> {
    if (text.isEmpty() || spans.isEmpty()) return emptyList()
    val charStyles = buildCharStyles(text.length, spans)
    return compressReminderStyles(charStyles)
}

fun trimReminderTextAndSpans(
    text: String,
    spans: List<ReminderStyleSpan>,
): Pair<String, List<ReminderStyleSpan>> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return "" to emptyList()
    if (trimmed == text) {
        return trimmed to normalizeReminderStyleSpans(text, spans)
    }

    val leadingTrim = text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
    val shifted = normalizeReminderStyleSpans(text, spans).mapNotNull { span ->
        ReminderStyleSpan(
            start = span.start - leadingTrim,
            end = span.end - leadingTrim,
            bold = span.bold,
            colorHex = span.colorHex,
        ).normalize(trimmed.length)
    }
    return trimmed to shifted
}

fun adjustReminderSpansForTextChange(
    oldText: String,
    newText: String,
    spans: List<ReminderStyleSpan>,
): List<ReminderStyleSpan> {
    if (oldText == newText) return normalizeReminderStyleSpans(newText, spans)
    if (oldText.isEmpty() || spans.isEmpty()) return emptyList()

    val prefixLength = commonPrefixLength(oldText, newText)
    val suffixLength = commonSuffixLength(oldText, newText, prefixLength)
    val oldChangeEnd = oldText.length - suffixLength
    val newChangeEnd = newText.length - suffixLength
    val delta = newChangeEnd - oldChangeEnd

    val adjusted = normalizeReminderStyleSpans(oldText, spans).flatMap { span ->
        when {
            span.end <= prefixLength -> listOf(span)
            span.start >= oldChangeEnd -> listOf(
                span.copy(
                    start = span.start + delta,
                    end = span.end + delta,
                )
            )
            span.start < prefixLength && span.end > oldChangeEnd -> listOfNotNull(
                ReminderStyleSpan(
                    start = span.start,
                    end = prefixLength,
                    bold = span.bold,
                    colorHex = span.colorHex,
                ).normalize(newText.length),
                ReminderStyleSpan(
                    start = newChangeEnd,
                    end = span.end + delta,
                    bold = span.bold,
                    colorHex = span.colorHex,
                ).normalize(newText.length),
            )
            span.start < prefixLength -> listOfNotNull(
                ReminderStyleSpan(
                    start = span.start,
                    end = prefixLength,
                    bold = span.bold,
                    colorHex = span.colorHex,
                ).normalize(newText.length)
            )
            span.end > oldChangeEnd -> listOfNotNull(
                ReminderStyleSpan(
                    start = newChangeEnd,
                    end = span.end + delta,
                    bold = span.bold,
                    colorHex = span.colorHex,
                ).normalize(newText.length)
            )
            else -> emptyList()
        }
    }
    return normalizeReminderStyleSpans(newText, adjusted)
}

fun toggleReminderHighlightOnSelection(
    text: String,
    spans: List<ReminderStyleSpan>,
    selection: TextRange,
): List<ReminderStyleSpan> {
    return applyReminderStyleToSelection(text, spans, selection) { current, isEntireSelectionStyled ->
        current.copy(
            bold = false,
            colorHex = if (isEntireSelectionStyled) null else ReminderHighlightRedHex,
        )
    }
}

fun summarizeReminderSelectionStyle(
    text: String,
    spans: List<ReminderStyleSpan>,
    selection: TextRange,
): ReminderSelectionStyle {
    val normalizedSelection = normalizeSelection(selection, text.length)
    if (normalizedSelection == null) {
        return ReminderSelectionStyle(
            hasSelection = false,
            highlighted = false,
        )
    }
    val charStyles = buildCharStyles(text.length, spans)
    var allHighlighted = true
    for (index in normalizedSelection.first until normalizedSelection.last) {
        val style = charStyles[index]
        if (style.colorHex != ReminderHighlightRedHex) {
            allHighlighted = false
        }
    }
    return ReminderSelectionStyle(
        hasSelection = true,
        highlighted = allHighlighted,
    )
}

private fun applyReminderStyleToSelection(
    text: String,
    spans: List<ReminderStyleSpan>,
    selection: TextRange,
    transform: (ReminderTextStyle, Boolean) -> ReminderTextStyle,
): List<ReminderStyleSpan> {
    val normalizedSelection = normalizeSelection(selection, text.length) ?: return spans
    val charStyles = buildCharStyles(text.length, spans).toMutableList()
    val selectionStyles = charStyles.subList(normalizedSelection.first, normalizedSelection.last)
    val entireSelectionStyled = selectionStyles.all { it.colorHex == ReminderHighlightRedHex }
    for (index in normalizedSelection.first until normalizedSelection.last) {
        charStyles[index] = transform(
            charStyles[index],
            entireSelectionStyled,
        )
    }
    return compressReminderStyles(charStyles)
}

private fun buildCharStyles(
    textLength: Int,
    spans: List<ReminderStyleSpan>,
): List<ReminderTextStyle> {
    val charStyles = MutableList(textLength) { DefaultReminderTextStyle }
    spans.mapNotNull { it.normalize(textLength) }.forEach { span ->
        for (index in span.start until span.end) {
            charStyles[index] = ReminderTextStyle(
                colorHex = span.colorHex ?: ReminderHighlightRedHex.takeIf { span.bold },
            )
        }
    }
    return charStyles
}

private fun compressReminderStyles(
    charStyles: List<ReminderTextStyle>,
): List<ReminderStyleSpan> {
    if (charStyles.isEmpty()) return emptyList()
    val spans = mutableListOf<ReminderStyleSpan>()
    var start = 0
    var current = charStyles.first()
    for (index in 1..charStyles.size) {
        val style = charStyles.getOrNull(index)
        if (style != current) {
            if (current != DefaultReminderTextStyle) {
                spans += ReminderStyleSpan(
                    start = start,
                    end = index,
                    bold = false,
                    colorHex = current.colorHex,
                )
            }
            if (index < charStyles.size) {
                start = index
                current = charStyles[index]
            }
        }
    }
    return spans
}

private fun ReminderStyleSpan.normalize(
    textLength: Int,
): ReminderStyleSpan? {
    val normalizedStart = start.coerceIn(0, textLength)
    val normalizedEnd = end.coerceIn(0, textLength)
    if (normalizedEnd <= normalizedStart) return null
    val normalizedColor = colorHex?.uppercase()
        ?: ReminderHighlightRedHex.takeIf { bold }
    if (normalizedColor.isNullOrBlank()) return null
    return copy(
        start = normalizedStart,
        end = normalizedEnd,
        bold = false,
        colorHex = normalizedColor,
    )
}

private fun normalizeSelection(
    selection: TextRange,
    textLength: Int,
): IntRange? {
    val start = min(selection.start, selection.end).coerceIn(0, textLength)
    val end = max(selection.start, selection.end).coerceIn(0, textLength)
    return if (end > start) start until end else null
}

fun parseReminderColorOrNull(colorHex: String?): Color? {
    if (colorHex.isNullOrBlank()) return null
    return runCatching {
        Color(android.graphics.Color.parseColor(colorHex))
    }.getOrNull()
}

private fun commonPrefixLength(
    oldText: String,
    newText: String,
): Int {
    val maxLength = min(oldText.length, newText.length)
    for (index in 0 until maxLength) {
        if (oldText[index] != newText[index]) return index
    }
    return maxLength
}

private fun commonSuffixLength(
    oldText: String,
    newText: String,
    prefixLength: Int,
): Int {
    val oldRemaining = oldText.length - prefixLength
    val newRemaining = newText.length - prefixLength
    val maxLength = min(oldRemaining, newRemaining)
    for (offset in 1..maxLength) {
        if (oldText[oldText.length - offset] != newText[newText.length - offset]) {
            return offset - 1
        }
    }
    return maxLength
}
