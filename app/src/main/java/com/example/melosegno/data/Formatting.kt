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

/** Valore attivo di una proprieta' alla posizione [index] (eredita dal lato sinistro). */
fun NoteData.effectiveFormatValue(index: Int, kind: FormatKind): Long? =
    formats.inheritedValueAt(index, kind)

/**
 * Valore della proprieta' coperto dall'inizio della selezione: a differenza
 * dell'ereditarieta' al cursore, uno span che finisce esattamente li' non conta,
 * perche' la selezione comincia con testo che non ne fa parte.
 */
fun NoteData.formatValueAtSelectionStart(index: Int, kind: FormatKind): Long? {
    var value: Long? = null
    formats.forEach { span ->
        if (span.kind == kind && span.start <= index && index < span.end) value = span.value
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

/**
 * Valore della proprieta' nel punto [index], preferendo gli span che finiscono
 * esattamente li' (la formattazione del carattere alla sinistra del cursore
 * vince su quella del carattere alla destra).
 */
private fun List<FormatSpan>.inheritedValueAt(index: Int, kind: FormatKind): Long? {
    var left: Long? = null
    var right: Long? = null
    forEach { span ->
        if (span.kind != kind) return@forEach
        if (span.start < index && index <= span.end) left = span.value
        else if (span.start <= index && index < span.end) right = span.value
    }
    return left ?: right
}

/** Le proprieta' scelte esplicitamente nel formato attivo. */
private fun FormatStyle.chosenKinds(): Set<FormatKind> = buildSet {
    textSizeSp?.let { add(FormatKind.SIZE) }
    color?.let { add(FormatKind.COLOR) }
    bold?.let { add(FormatKind.BOLD) }
    italic?.let { add(FormatKind.ITALIC) }
    underline?.let { add(FormatKind.UNDERLINE) }
    strike?.let { add(FormatKind.STRIKE) }
}

/**
 * Ordina gli span, scarta gli intervalli vuoti e unisce quelli adiacenti o
 * sovrapposti con la stessa proprieta' e lo stesso valore.
 */
private fun List<FormatSpan>.merged(): List<FormatSpan> =
    groupBy { it.kind to it.value }
        .flatMap { (_, group) ->
            group.sortedBy { it.start }.fold(mutableListOf<FormatSpan>()) { acc, span ->
                val last = acc.lastOrNull()
                if (last != null && span.start <= last.end) {
                    acc[acc.lastIndex] = last.copy(end = maxOf(last.end, span.end))
                } else {
                    acc.add(span)
                }
                acc
            }
        }
        .filter { it.start < it.end }
        .sortedBy { it.start }

/** Toglie dalla lista gli span di [kind] che intersecano [from, to), spezzandoli se servisse. */
fun List<FormatSpan>.withoutKindInRange(kind: FormatKind, from: Int, to: Int): List<FormatSpan> {
    val result = mutableListOf<FormatSpan>()
    // Ciclo esplicito: dentro buildList la forEach risolverebbe il receiver
    // della lista in costruzione (vuota) e non quello della lista di partenza.
    for (span in this) {
        if (span.kind != kind) {
            result.add(span)
        } else {
            if (span.start < from) result.add(span.copy(end = minOf(span.end, from)))
            if (span.end > to) result.add(span.copy(start = maxOf(span.start, to)))
        }
    }
    return result
}

/** Applica una proprieta' di formattazione alla selezione [start, end). */
fun NoteData.withFormatApplied(start: Int, end: Int, kind: FormatKind, value: Long): NoteData {
    if (start >= end) return this
    val cleaned = formats.withoutKindInRange(kind, start, end)
    return copy(formats = (cleaned + FormatSpan(start, end, kind, value)).merged())
}

/**
 * Aggiorna gli span dopo una modifica al testo.
 *
 * La zona modificata e' [prefix, oldEnd) nel vecchio testo e [prefix, newEnd)
 * nel nuovo. Gli span che la intersecano si spezzano: la parte prima resta,
 * la parte dopo si sposta della differenza di lunghezza, il testo sostituito
 * perde il tratto. Il testo inserito prende il formato attivo; per le
 * proprieta' non scelte eredita il formato presente nel punto della modifica.
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

    val result = formats.flatMap { span ->
        when {
            // Tutto prima della modifica: resta com'e'.
            span.end <= prefix -> listOf(span)
            // Tutto dopo: traslato della differenza di lunghezza.
            span.start >= oldEnd -> listOf(span.copy(start = span.start + delta, end = span.end + delta))
            // Interseca la modifica: si spezza alle due estremita'.
            else -> buildList {
                if (span.start < prefix) add(span.copy(end = prefix))
                if (span.end > oldEnd) add(span.copy(start = newEnd, end = span.end + delta))
            }
        }
    }.toMutableList()

    if (newEnd > prefix) {
        result += active.toSpans(prefix, newEnd)
        val chosen = active.chosenKinds()
        FormatKind.entries.filter { it !in chosen }.forEach { kind ->
            formats.inheritedValueAt(prefix, kind)?.let { value ->
                result += FormatSpan(prefix, newEnd, kind, value)
            }
        }
    }
    return result.merged()
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
