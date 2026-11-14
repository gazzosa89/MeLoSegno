package com.example.melosegno.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/** Proprieta' di formattazione applicabile a un tratto di testo. */
enum class FormatKind { SIZE, COLOR, BOLD, ITALIC, UNDERLINE, STRIKE }

/** Un intervallo [start, end) del testo con una proprieta' di formattazione.
 *  SIZE: valore in sp; COLOR: ARGB (0 = predefinito); i toggle: 1 = attivo, 0 = disattivo. */
data class FormatSpan(val start: Int, val end: Int, val kind: FormatKind, val value: Long)

/**
 * Formato "attivo": quello che verra' applicato al testo digitato da ora in poi.
 * Una proprieta' null significa "nessuna scelta: usa il valore predefinito".
 */
data class FormatStyle(
    val textSizeSp: Int? = null,
    val color: Long? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val strike: Boolean? = null
) {
    fun toggleValue(kind: FormatKind): Boolean? = when (kind) {
        FormatKind.BOLD -> bold
        FormatKind.ITALIC -> italic
        FormatKind.UNDERLINE -> underline
        FormatKind.STRIKE -> strike
        else -> null
    }

    fun toggled(kind: FormatKind, on: Boolean): FormatStyle = when (kind) {
        FormatKind.BOLD -> copy(bold = on)
        FormatKind.ITALIC -> copy(italic = on)
        FormatKind.UNDERLINE -> copy(underline = on)
        FormatKind.STRIKE -> copy(strike = on)
        else -> this
    }

    fun toSpans(start: Int, end: Int): List<FormatSpan> = buildList {
        textSizeSp?.let { add(FormatSpan(start, end, FormatKind.SIZE, it.toLong())) }
        color?.let { add(FormatSpan(start, end, FormatKind.COLOR, it)) }
        bold?.let { add(FormatSpan(start, end, FormatKind.BOLD, if (it) 1L else 0L)) }
        italic?.let { add(FormatSpan(start, end, FormatKind.ITALIC, if (it) 1L else 0L)) }
        underline?.let { add(FormatSpan(start, end, FormatKind.UNDERLINE, if (it) 1L else 0L)) }
        strike?.let { add(FormatSpan(start, end, FormatKind.STRIKE, if (it) 1L else 0L)) }
    }
}

/** Valore attivo di una proprieta' alla posizione [index] (il confine destro eredita). */
fun NoteData.effectiveFormatValue(index: Int, kind: FormatKind): Long? {
    var value: Long? = null
    formats.forEach { span ->
        if (span.kind == kind && index >= span.start && index <= span.end) value = span.value
    }
    return value
}

/** Formato attivo alla posizione [index], ricavato dagli span gia' presenti. */
fun NoteData.activeFormatAt(index: Int): FormatStyle = FormatStyle(
    textSizeSp = effectiveFormatValue(index, FormatKind.SIZE)?.toInt(),
    color = effectiveFormatValue(index, FormatKind.COLOR),
    bold = effectiveFormatValue(index, FormatKind.BOLD)?.let { it == 1L },
    italic = effectiveFormatValue(index, FormatKind.ITALIC)?.let { it == 1L },
    underline = effectiveFormatValue(index, FormatKind.UNDERLINE)?.let { it == 1L },
    strike = effectiveFormatValue(index, FormatKind.STRIKE)?.let { it == 1L }
)

/** Toglie dalla lista gli span di [kind] che intersecano [from, to), spezzandoli se servisse. */
fun List<FormatSpan>.withoutKindInRange(kind: FormatKind, from: Int, to: Int): List<FormatSpan> =
    buildList {
        forEach { span ->
            if (span.kind != kind) {
                add(span)
            } else {
                if (span.start < from) add(span.copy(end = minOf(span.end, from)))
                if (span.end > to) add(span.copy(start = maxOf(span.start, to)))
            }
        }
    }

/** Applica una proprieta' di formattazione alla selezione [start, end). */
fun NoteData.withFormatApplied(start: Int, end: Int, kind: FormatKind, value: Long): NoteData {
    if (start >= end) return this
    val cleaned = formats.withoutKindInRange(kind, start, end)
    return copy(formats = cleaned + FormatSpan(start, end, kind, value))
}

/**
 * Aggiorna gli span dopo una modifica al testo: il testo sostituito perde la
 * vecchia formattazione, quello inserito riceve il formato attivo, il resto
 * viene spostato della differenza di lunghezza.
 */
fun applyTextEdit(
    oldText: String,
    formats: List<FormatSpan>,
    newText: String,
    active: FormatStyle
): List<FormatSpan> {
    if (oldText == newText) return formats

    var prefix = 0
    val commonLength = minOf(oldText.length, newText.length)
    while (prefix < commonLength && oldText[prefix] == newText[prefix]) prefix++
    var oldEnd = oldText.length
    var newEnd = newText.length
    while (oldEnd > prefix && newEnd > prefix && oldText[oldEnd - 1] == newText[newEnd - 1]) {
        oldEnd--
        newEnd--
    }
    val delta = newEnd - oldEnd

    val result = formats.mapNotNull { span ->
        when {
            span.end <= prefix -> span
            span.start >= oldEnd -> span.copy(start = span.start + delta, end = span.end + delta)
            else -> if (span.start < prefix) span.copy(end = prefix) else null
        }
    }.toMutableList()

    if (newEnd > prefix) {
        result += active.toSpans(prefix, newEnd)
    }
    return result.filter { it.start < it.end }
}

/** Testo con la formattazione applicata, per la visualizzazione. */
fun String.annotated(formats: List<FormatSpan>): AnnotatedString = buildAnnotatedString {
    append(this@annotated)
    formats.forEach { span ->
        val start = span.start.coerceIn(0, length)
        val end = span.end.coerceIn(0, length)
        if (start >= end) return@forEach
        when (span.kind) {
            FormatKind.SIZE -> addStyle(SpanStyle(fontSize = span.value.toInt().sp), start, end)
            FormatKind.COLOR -> addStyle(
                SpanStyle(color = if (span.value == 0L) Color.Unspecified else Color(span.value)),
                start,
                end
            )
            FormatKind.BOLD -> addStyle(
                SpanStyle(fontWeight = if (span.value == 1L) FontWeight.Bold else FontWeight.Normal),
                start,
                end
            )
            FormatKind.ITALIC -> addStyle(
                SpanStyle(fontStyle = if (span.value == 1L) FontStyle.Italic else FontStyle.Normal),
                start,
                end
            )
            FormatKind.UNDERLINE -> addStyle(
                SpanStyle(textDecoration = if (span.value == 1L) TextDecoration.Underline else TextDecoration.None),
                start,
                end
            )
            FormatKind.STRIKE -> addStyle(
                SpanStyle(textDecoration = if (span.value == 1L) TextDecoration.LineThrough else TextDecoration.None),
                start,
                end
            )
        }
    }
}
