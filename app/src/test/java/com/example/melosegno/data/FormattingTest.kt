package com.example.melosegno.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test della logica di formattazione: applicazione a selezione, sincronizzazione
 * degli span dopo le modifiche al testo, ereditarieta' al cursore.
 */
class FormattingTest {

    // ---------- Helper ----------

    private fun bold(s: Int, e: Int, v: Long = 1L) = FormatSpan(s, e, FormatKind.BOLD, v)
    private fun size(s: Int, e: Int, v: Int) = FormatSpan(s, e, FormatKind.SIZE, v.toLong())
    private fun color(s: Int, e: Int, v: Long) = FormatSpan(s, e, FormatKind.COLOR, v)

    private fun noteOf(text: String, vararg spans: FormatSpan) =
        NoteData(text = text, formats = spans.toList())

    private fun edit(
        oldText: String,
        spans: List<FormatSpan>,
        newText: String,
        active: FormatStyle = FormatStyle()
    ): List<FormatSpan> = applyTextEdit(oldText, spans, newText, active)

    private fun List<FormatSpan>.normalized(): List<FormatSpan> =
        sortedWith(compareBy({ it.start }, { it.end }, { it.kind }, { it.value }))

    // ---------- applyTextEdit: cancellazioni ----------

    @Test
    fun deletionAtStartKeepsFormattingOfTheRest() {
        // Bug storico: cancellare il primo carattere azzerava tutta la formattazione.
        val result = edit("abcde", listOf(bold(0, 5)), "bcde")
        assertEquals(listOf(bold(0, 4)), result.normalized())
    }

    @Test
    fun deletionInsideSpanKeepsBothSides() {
        // "abcdef" tutto in grassetto, via "cd": "abef" resta tutto in grassetto.
        val result = edit("abcdef", listOf(bold(0, 6)), "abef")
        assertEquals(listOf(bold(0, 4)), result.normalized())
    }

    @Test
    fun deletionOfSpannedSelectionRemovesOnlyThatSpan() {
        // "cd" era l'unico tratto non in grassetto: dopo la cancellazione tutto
        // il testo restante e' in grassetto e gli span adiacenti si uniscono.
        val result = edit("abcdef", listOf(bold(0, 2), bold(2, 4), bold(4, 6)), "abef")
        assertEquals(listOf(bold(0, 4)), result.normalized())
    }

    @Test
    fun deletionSpanThatEndsAfterTheRegionKeepsItsTail() {
        // "abcdef" grassetto [0,6), via "cd" (2..4): il tratto dopo la cancellazione
        // ("ef") non deve perdere il grassetto.
        val result = edit("abcdef", listOf(bold(0, 6)), "abef")
        assertEquals(listOf(bold(0, 4)), result.normalized())
    }

    @Test
    fun deletionBetweenSpansKeepsBothSpans() {
        // "ab" grassetto, "cd" corsivo, "ef" grassetto: via "cd"; i due tratti
        // in grassetto adiacenti si uniscono in uno solo.
        val result = edit(
            "abcdef",
            listOf(bold(0, 2), FormatSpan(2, 4, FormatKind.ITALIC, 1L), bold(4, 6)),
            "abef"
        )
        assertEquals(listOf(bold(0, 4)), result.normalized())
    }

    // ---------- applyTextEdit: inserimenti ----------

    @Test
    fun insertionInsideSpanKeepsTheSpanWhole() {
        // Digitare dentro un tratto in grassetto non deve spezzarlo.
        val result = edit("abcd", listOf(bold(0, 4)), "abXcd")
        assertEquals(listOf(bold(0, 5)), result.normalized())
    }

    @Test
    fun insertionInheritsSpannedFormatWhenActiveIsNull() {
        // Formato attivo non scelto: il testo inserito eredita dal punto del cursore.
        val result = edit("ab", listOf(bold(0, 2)), "abX")
        assertEquals(listOf(bold(0, 3)), result.normalized())
    }

    @Test
    fun insertionUsesActiveFormatOverTheInherited() {
        // Tratto in grassetto, utente ha tolto il grassetto attivo: il carattere
        // nuovo non e' in grassetto, il resto si'.
        val result = edit("abcd", listOf(bold(0, 4)), "abXcd", FormatStyle(bold = false))
        assertEquals(
            listOf(bold(0, 2), bold(2, 3, 0L), bold(3, 5)),
            result.normalized()
        )
    }

    @Test
    fun insertionInheritsSizeWhenActiveIsNull() {
        val result = edit("ab", listOf(size(0, 2, 20)), "abX")
        assertEquals(listOf(size(0, 3, 20)), result.normalized())
    }

    @Test
    fun insertionWithActiveSizeOverridesInherited() {
        val result = edit("ab", listOf(size(0, 2, 20)), "abX", FormatStyle(textSizeSp = 12))
        assertEquals(
            listOf(size(0, 2, 20), size(2, 3, 12)),
            result.normalized()
        )
    }

    @Test
    fun insertionAtEmptyNoteTakesActiveFormat() {
        val result = edit("", emptyList(), "X", FormatStyle(bold = true))
        assertEquals(listOf(bold(0, 1)), result.normalized())
    }

    @Test
    fun insertionAfterUnformattedTextDoesNotInherit() {
        // "ab" grassetto, poi uno spazio non formattato: digitare dopo lo spazio
        // non deve riprendere il grassetto di prima.
        val result = edit("ab ", listOf(bold(0, 2)), "ab X")
        assertEquals(listOf(bold(0, 2)), result.normalized())
    }

    // ---------- applyTextEdit: sostituzioni e spostamenti ----------

    @Test
    fun replacementKeepsSpanAroundAndShiftsTail() {
        // "abcdef" grassetto [0,6), sostituzione di "cd" con "XY": tutto resta in
        // grassetto (il testo nuovo eredita).
        val result = edit("abcdef", listOf(bold(0, 6)), "abXYef")
        assertEquals(listOf(bold(0, 6)), result.normalized())
    }

    @Test
    fun replacementOfUnformattedTextTakesActiveFormat() {
        val result = edit("abcdef", emptyList(), "abXYef", FormatStyle(color = 0xFF0000FF))
        assertEquals(listOf(color(2, 4, 0xFF0000FF)), result.normalized())
    }

    @Test
    fun spansAfterTheEditShiftByTheLengthDifference() {
        val result = edit("abcdef", listOf(bold(4, 6)), "abXcdef")
        assertEquals(listOf(bold(5, 7)), result.normalized())
    }

    @Test
    fun identicalTextLeavesFormatsUnchanged() {
        val spans = listOf(bold(0, 3))
        assertEquals(spans, edit("abc", spans, "abc"))
    }

    @Test
    fun repeatedCharacterInsertionAtEnd() {
        // Prefisso e suffisso comuni si sovrapporrebbero: il conteggio deve restare
        // corretto ("aa" -> "aaa" e' un inserimento alla fine).
        val result = edit("aa", listOf(bold(0, 2)), "aaa")
        assertEquals(listOf(bold(0, 3)), result.normalized())
    }

    @Test
    fun repeatedCharacterDeletionAtEnd() {
        val result = edit("aaa", listOf(bold(0, 3)), "aa")
        assertEquals(listOf(bold(0, 2)), result.normalized())
    }

    @Test
    fun multilineInsertionInheritsAtCaret() {
        // Inserimento di "X\n" dentro un tratto in grassetto: il tratto copre
        // tutto il testo nuovo, parti spezzate e testo inserito si uniscono.
        val result = edit("ab\ncd", listOf(bold(0, 5)), "ab\nX\ncd")
        assertEquals(listOf(bold(0, 7)), result.normalized())
    }

    // ---------- withFormatApplied e withoutKindInRange ----------

    @Test
    fun applyingOverPartOfASpanSplitsIt() {
        val note = noteOf("abcdef", bold(0, 6)).withFormatApplied(2, 4, FormatKind.BOLD, 0L)
        assertEquals(
            listOf(bold(0, 2), bold(2, 4, 0L), bold(4, 6)),
            note.formats.normalized()
        )
    }

    @Test
    fun applyingKeepsOtherKinds() {
        val note = noteOf("abcdef", bold(0, 6), color(0, 6, 0xFF0000FF))
            .withFormatApplied(1, 3, FormatKind.BOLD, 0L)
        assertEquals(
            listOf(
                bold(0, 1), color(0, 6, 0xFF0000FF),
                bold(1, 3, 0L), bold(3, 6)
            ),
            note.formats.normalized()
        )
    }

    @Test
    fun applyingToEmptySelectionChangesNothing() {
        val note = noteOf("abc", bold(0, 3)).withFormatApplied(1, 1, FormatKind.BOLD, 1L)
        assertEquals(listOf(bold(0, 3)), note.formats)
    }

    @Test
    fun removingKindInRangeSplitsTheSpan() {
        val cleaned = listOf(bold(0, 6)).withoutKindInRange(FormatKind.BOLD, 2, 4)
        assertEquals(listOf(bold(0, 2), bold(4, 6)), cleaned.normalized())
    }

    // ---------- ereditarieta' al cursore e alla selezione ----------

    @Test
    fun effectiveValueInheritsAtRightBoundaryForTyping() {
        // Il cursore a fine tratto in grassetto continua il grassetto.
        assertEquals(1L, noteOf("abc", bold(0, 3)).effectiveFormatValue(3, FormatKind.BOLD))
    }

    @Test
    fun selectionStartDoesNotInheritFromSpanEndingExactlyThere() {
        // Bug: selezionare testo dopo un tratto in grassetto lo riteneva gia'
        // in grassetto (ereditava dal confine sinistro) e il toggle lo spegneva.
        assertEquals(null, noteOf("abcde", bold(0, 2)).formatValueAtSelectionStart(2, FormatKind.BOLD))
        assertEquals(1L, noteOf("abcde", bold(0, 5)).formatValueAtSelectionStart(2, FormatKind.BOLD))
        assertEquals(1L, noteOf("abcde", bold(2, 5)).formatValueAtSelectionStart(2, FormatKind.BOLD))
        assertEquals(null, noteOf("abcde", bold(0, 2), bold(4, 5)).formatValueAtSelectionStart(2, FormatKind.BOLD))
    }

    @Test
    fun activeFormatAtEndOfNoteReflectsLastSpan() {
        val style = noteOf("abcd", bold(0, 2), color(2, 4, 0xFF0000FF)).activeFormatAt(4)
        assertEquals(0xFF0000FF, style.color)
        assertEquals(null, style.bold)
    }

    // ---------- rendering ----------

    @Test
    fun annotatedIgnoresOutOfRangeSpansWithoutCrashing() {
        val text = "abc".annotated(listOf(bold(0, 10)))
        assertEquals("abc", text.text)
    }

    @Test
    fun annotatedWithEmptyTextAndSpansIsEmpty() {
        val text = "".annotated(listOf(bold(0, 3)))
        assertEquals("", text.text)
    }

    // ---------- unione degli span ----------

    @Test
    fun adjacentSameValueSpansAreMergedAfterEdits() {
        // Dopo la correzione, spezzare e riunire non deve lasciare span inutili.
        val result = edit("abcd", listOf(bold(0, 4)), "abXcd")
        assertEquals(listOf(bold(0, 5)), result.normalized())
        assertTrue(result.all { it.start < it.end })
    }
}
