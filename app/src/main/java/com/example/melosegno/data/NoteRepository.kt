package com.example.melosegno.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Contenuto di una nota.
 *
 * Una nota puo' contenere testo, tratti disegnati a mano o entrambi
 * (restano sovrapposti e sempre visibili).
 *
 * - La formattazione del testo vive in [formats]: span per intervallo, cosi'
 *   le scelte valgono solo per il testo nuovo o per la selezione.
 * - Ogni tratto porta il proprio spessore e colore, fissati al momento del
 *   disegno: le modifiche agli strumenti valgono solo per i tratti nuovi.
 *
 * [id] uguale a 0 significa "nota nuova, mai salvata".
 * I colori sono valori ARGB; 0L significa "usa il colore del tema".
 */
data class NoteData(
    val id: Long = 0L,
    // Titolo scelto al primo salvataggio; vuoto nelle note piu' vecchie.
    val title: String = "",
    val text: String = "",
    val formats: List<FormatSpan> = emptyList(),
    val strokes: List<Stroke> = emptyList(),
    // Strumenti correnti per i NUOVI tratti
    val strokeSizeDp: Float = DEFAULT_STROKE_SIZE_DP,
    val inkColor: Long = 0L
) {
    fun isEmpty(): Boolean = text.isBlank() && strokes.isEmpty()

    companion object {
        const val DEFAULT_STROKE_SIZE_DP = 4f
    }
}

/** Un tratto disegnato a mano: coordinate piatte [x1, y1, x2, y2, ...] + aspetto. */
data class Stroke(
    val points: List<Float>,
    val widthDp: Float = NoteData.DEFAULT_STROKE_SIZE_DP,
    val color: Long = 0L
)

/**
 * Persistenza minimale su file JSON nella cartella files/notes.
 * Nessun database, nessuna libreria esterna.
 */
class NoteRepository(context: Context) {

    private val notesDir = File(context.filesDir, "notes").apply { mkdirs() }

    /** Elenco delle note, dalla piu' recente alla piu' vecchia. */
    fun listNotes(): List<NoteData> =
        notesDir.listFiles { file -> file.name.endsWith(".json") }
            ?.sortedByDescending { it.name }
            ?.mapNotNull { file -> runCatching { readNote(file) }.getOrNull() }
            ?: emptyList()

    /** Salva la nota; se e' nuova le assegna un id e la restituisce aggiornata. */
    fun saveNote(note: NoteData): NoteData {
        val id = if (note.id == 0L) System.currentTimeMillis() else note.id
        val json = JSONObject().apply {
            put(KEY_TITLE, note.title)
            put(KEY_TEXT, note.text)
            put(KEY_FORMATS, JSONArray(note.formats.map { span ->
                JSONObject()
                    .put("s", span.start)
                    .put("e", span.end)
                    .put("k", span.kind.name)
                    .put("v", span.value)
            }))
            put(KEY_STROKES, JSONArray(note.strokes.map { stroke ->
                JSONObject()
                    .put("p", JSONArray(stroke.points))
                    .put("w", stroke.widthDp.toDouble())
                    .put("c", stroke.color)
            }))
            put(KEY_STROKE_SIZE, note.strokeSizeDp.toDouble())
            put(KEY_INK_COLOR, note.inkColor)
        }
        fileFor(id).writeText(json.toString())
        return note.copy(id = id)
    }

    fun deleteNote(id: Long) {
        fileFor(id).delete()
    }

    private fun fileFor(id: Long) = File(notesDir, "note_$id.json")

    private fun readNote(file: File): NoteData {
        val json = JSONObject(file.readText())
        val text = json.optString(KEY_TEXT)

        val formats = mutableListOf<FormatSpan>()
        json.optJSONArray(KEY_FORMATS)?.let { arr ->
            (0 until arr.length()).forEach { i ->
                val span = arr.optJSONObject(i) ?: return@forEach
                val kind = runCatching { FormatKind.valueOf(span.optString("k")) }.getOrNull()
                    ?: return@forEach
                val start = span.optInt("s")
                val end = span.optInt("e")
                if (start < end && end <= text.length + 1) {
                    formats += FormatSpan(start, end, kind, span.optLong("v"))
                }
            }
        }

        val strokes = mutableListOf<Stroke>()
        json.optJSONArray(KEY_STROKES)?.let { arr ->
            (0 until arr.length()).forEach { i ->
                when (val item = arr.opt(i)) {
                    // Formato attuale: oggetto con punti, spessore e colore
                    is JSONObject -> {
                        val points = item.optJSONArray("p")
                            ?.let { p -> (0 until p.length()).map { p.getDouble(it).toFloat() } }
                            ?: return@forEach
                        strokes += Stroke(
                            points = points,
                            widthDp = item.optDouble("w", NoteData.DEFAULT_STROKE_SIZE_DP.toDouble()).toFloat(),
                            color = item.optLong("c", 0L)
                        )
                    }
                    // Formato storico: solo array di punti
                    is JSONArray -> {
                        val points = (0 until item.length()).map { item.getDouble(it).toFloat() }
                        strokes += Stroke(points = points)
                    }
                }
            }
        }

        return NoteData(
            id = file.name.removePrefix(FILE_PREFIX).removeSuffix(".json").toLong(),
            title = json.optString(KEY_TITLE),
            text = text,
            formats = formats,
            strokes = strokes,
            strokeSizeDp = json.optDouble(KEY_STROKE_SIZE, NoteData.DEFAULT_STROKE_SIZE_DP.toDouble()).toFloat(),
            inkColor = json.optLong(KEY_INK_COLOR, 0L)
        )
    }

    private companion object {
        const val FILE_PREFIX = "note_"
        const val KEY_TITLE = "title"
        const val KEY_TEXT = "text"
        const val KEY_FORMATS = "formats"
        const val KEY_STROKES = "strokes"
        const val KEY_STROKE_SIZE = "strokeSizeDp"
        const val KEY_INK_COLOR = "inkColor"
    }
}
