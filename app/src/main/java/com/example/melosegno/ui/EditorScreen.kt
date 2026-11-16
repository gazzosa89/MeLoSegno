package com.example.melosegno.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.melosegno.R
import com.example.melosegno.data.AppSettings
import com.example.melosegno.data.FormatKind
import com.example.melosegno.data.FormatSpan
import com.example.melosegno.data.FormatStyle
import com.example.melosegno.data.LanguageSetting
import com.example.melosegno.data.NoteData
import com.example.melosegno.data.NoteRepository
import com.example.melosegno.data.PinnedNoteStore
import com.example.melosegno.data.SettingsRepository
import com.example.melosegno.data.Stroke
import com.example.melosegno.data.ThemeSetting
import com.example.melosegno.data.WritingMode
import com.example.melosegno.data.activeFormatAt
import com.example.melosegno.data.annotated
import com.example.melosegno.data.applyTextEdit
import com.example.melosegno.data.effectiveFormatValue
import com.example.melosegno.data.formatValueAtSelectionStart
import com.example.melosegno.data.withFormatApplied
import com.example.melosegno.widget.PinnedNoteWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dimensioni scelte dal menu della barra strumenti. */
private val TEXT_SIZES = listOf(14, 16, 20, 24)

private data class ColorOption(val value: Long)

private val NOTE_COLORS = listOf(
    ColorOption(0xFF1C1B1F), // nero
    ColorOption(0xFF616161), // grigio
    ColorOption(0xFF1565C0), // blu
    ColorOption(0xFF00838F), // petrolio
    ColorOption(0xFF2E7D32), // verde
    ColorOption(0xFFC62828), // rosso
    ColorOption(0xFFEF6C00), // arancione
    ColorOption(0xFFF9A825), // giallo
    ColorOption(0xFF6A1B9A), // viola
    ColorOption(0xFFAD1457), // rosa
    ColorOption(0xFFFFFFFF)  // bianco
)

private data class StrokeSizeOption(val labelRes: Int, val valueDp: Float)

private val STROKE_SIZES = listOf(
    StrokeSizeOption(R.string.size_thin, 2f),
    StrokeSizeOption(R.string.size_normal, 4f),
    StrokeSizeOption(R.string.size_thick, 7f),
    StrokeSizeOption(R.string.size_extra_thick, 12f)
)

/** Raggio della gomma, 1:1 con lo spessore del tratto scelto. */
private const val ERASER_RADIUS_SCALE = 1f

/** Larghezza massima del contenuto: su schermi larghi resta leggibile. */
private val MAX_CONTENT_WIDTH = 640.dp

/** Lunghezza massima del titolo di una nota. */
private const val MAX_TITLE_LENGTH = 40

/** Prefissi degli elenchi nella modalità testo. */
private const val BULLET_PREFIX = "• "
private val NUMBERED_PREFIX = Regex("^(\\d+)\\. ")

/** Marcatore di elenco trovato a inizio riga: tipo e lunghezza del prefisso. */
private data class ListMark(val numbered: Boolean, val prefixLength: Int)

private fun listMarkerOf(line: String): ListMark? = when {
    line.startsWith(BULLET_PREFIX) -> ListMark(false, BULLET_PREFIX.length)
    else -> NUMBERED_PREFIX.find(line)?.let { ListMark(true, it.value.length) }
}

/** Inizio della riga che contiene la posizione [index]. */
private fun lineStart(text: String, index: Int): Int {
    if (index <= 0) return 0
    val nl = text.lastIndexOf('\n', index - 1)
    return if (nl < 0) 0 else nl + 1
}

/** Fine della riga che contiene la posizione [index] (escluso il ritorno a capo). */
private fun lineEnd(text: String, index: Int): Int {
    if (index >= text.length) return text.length
    val nl = text.indexOf('\n', index)
    return if (nl < 0) text.length else nl
}

/** Tutte le righe non vuote della selezione portano gia' quel tipo di elenco? */
private fun selectionMarked(text: String, selection: TextRange, numbered: Boolean): Boolean {
    if (text.isEmpty()) return false
    val selStart = selection.min.coerceIn(0, text.length)
    val selEnd = selection.max.coerceIn(0, text.length)
    val from = lineStart(text, selStart)
    val to = lineEnd(text, (selEnd - 1).coerceAtLeast(selStart))
    val lines = text.substring(from, to).split('\n').filter { it.isNotBlank() }
    if (lines.isEmpty()) return false
    return lines.all { listMarkerOf(it)?.numbered == numbered }
}

/**
 * Schermata unica dell'app: all'apertura c'e' sempre una nota nuova e vuota,
 * pronta per essere scritta nella modalita' predefinita delle impostazioni.
 * Testo e tratti vivono nella stessa nota: restano entrambi visibili,
 * sovrapposti, in qualunque modalita'.
 */
@Composable
fun MeLoSegnoApp(
    repository: NoteRepository,
    settingsRepo: SettingsRepository,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    // Nota da aprire all'avvio, richiesta dal widget della nota fissata.
    openNoteRequest: Long = 0L,
    onOpenNoteHandled: () -> Unit = {}
) {
    var note by remember { mutableStateOf(NoteData()) }
    var mode by remember { mutableStateOf(settings.defaultMode) }
    var eraserOn by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    // Testo + selezione: vissuto qui per non perdere il cursore cambiando modalita'.
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    // Formato attivo: applicato al testo digitato da ora in poi.
    var activeFormat by remember { mutableStateOf(FormatStyle()) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteData?>(null) }
    // Rinomina del titolo direttamente dall'elenco delle note salvate.
    var noteToRename by remember { mutableStateOf<NoteData?>(null) }
    // Al primo salvataggio si chiede il titolo della nota.
    var showTitleDialog by remember { mutableStateOf(false) }
    var savedNotes by remember { mutableStateOf(emptyList<NoteData>()) }
    // Altezza dell'area visibile tra le barre: il foglio e' almeno cosi' alta.
    var viewportHeightPx by remember { mutableStateOf(0) }
    // Layout del testo in editor: serve a far seguire il cursore al foglio.
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // All'apertura di una nota il foglio resta dov'e': niente salto al cursore.
    var suppressCaretFollow by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    // Nota fissata nel widget: 0 = nessuna.
    val pinnedStore = remember { PinnedNoteStore(context) }
    var pinnedId by remember { mutableLongStateOf(pinnedStore.pinnedNoteId()) }
    // Fissaggio richiesto ma widget assente: si chiede se aggiungerlo.
    var noteToPinWithoutWidget by remember { mutableStateOf<NoteData?>(null) }

    // Tasto indietro col menu aperto: chiude il menu invece di uscire dall'app.
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    // Raggio della gomma, uguale allo spessore del tratto scelto.
    val densityPxPerDp = LocalDensity.current.density
    val eraseRadiusPx = with(LocalDensity.current) { (note.strokeSizeDp * ERASER_RADIUS_SCALE).dp.toPx() }
    val textFocusRequester = remember { FocusRequester() }

    // Ricarica l'elenco ogni volta che il menu viene aperto.
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            savedNotes = withContext(Dispatchers.IO) { repository.listNotes() }
        }
    }

    // All'apertura, e a ogni ritorno in modalita' testo, il campo e' pronto a scrivere.
    LaunchedEffect(mode) {
        if (mode == WritingMode.TEXT) textFocusRequester.requestFocus()
    }

    fun updateSettings(updated: AppSettings) {
        settingsRepo.save(updated)
        // Etichetta o colore dell'icona cambiati: si attiva l'alias giusto.
        if (updated.useShortLabel != settings.useShortLabel ||
            updated.iconColorKey != settings.iconColorKey
        ) {
            settingsRepo.applyIconLabel(context, updated)
        }
        onSettingsChange(updated)
    }

    fun saveCurrentNote() {
        scope.launch {
            note = withContext(Dispatchers.IO) { repository.saveNote(note) }
            Toast.makeText(context, R.string.saved_toast, Toast.LENGTH_SHORT).show()
            // Il widget mostra la nota fissata: se e' quella salvata, aggiornalo.
            if (pinnedId == note.id) {
                withContext(Dispatchers.IO) { PinnedNoteWidget.updateAll(context) }
            }
        }
    }

    /** Fissa o sfissa la nota nel widget della home. */
    fun pinNote(target: NoteData) {
        if (pinnedId == target.id) {
            // La nota e' gia' fissata: la sfissa.
            pinnedId = 0L
            pinnedStore.unpin()
            scope.launch {
                withContext(Dispatchers.IO) { PinnedNoteWidget.updateAll(context) }
            }
            return
        }
        // Il widget deve esistere gia' nella schermata home: se non c'e',
        // si chiede all'utente se aggiungerlo.
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(ComponentName(context, PinnedNoteWidget::class.java))
        if (widgetIds.isNotEmpty()) {
            pinnedId = target.id
            pinnedStore.pin(target.id)
            scope.launch {
                withContext(Dispatchers.IO) { PinnedNoteWidget.updateAll(context) }
            }
        } else {
            noteToPinWithoutWidget = target
        }
    }

    /** Scarta il documento e apre una nota nuova nella modalita' predefinita. */
    fun discardCurrentNote() {
        note = NoteData()
        textFieldValue = TextFieldValue("")
        activeFormat = FormatStyle()
        mode = settings.defaultMode
        eraserOn = false
    }

    fun openSavedNote(loaded: NoteData) {
        note = loaded
        // Il cursore parte dall'inizio: il focus automatico del campo in
        // modalita' testo non deve far scorrere il foglio fino in fondo.
        textFieldValue = TextFieldValue(loaded.text, selection = TextRange(0))
        // Il foglio si apre dov'e': il cursore in fondo non deve far saltare
        // lo scroll alla fine della nota.
        suppressCaretFollow = true
        activeFormat = loaded.activeFormatAt(0)
        mode = if (loaded.strokes.isNotEmpty()) WritingMode.HAND else WritingMode.TEXT
        eraserOn = false
        scope.launch { drawerState.close() }
    }

    // Il widget ha chiesto di aprire una nota: si carica e si apre.
    LaunchedEffect(openNoteRequest) {
        if (openNoteRequest != 0L) {
            val loaded = withContext(Dispatchers.IO) { repository.loadNote(openNoteRequest) }
            if (loaded != null) openSavedNote(loaded)
            onOpenNoteHandled()
        }
    }

    // ---------- Modifiche al testo con gestione della formattazione ----------

    fun onTextFieldChange(newValue: TextFieldValue) {
        var value = newValue
        // Se si preme Invio a fine riga di un elenco, la riga nuova parte gia'
        // con il marcatore: puntato, o numero incrementato di uno.
        if (value.text.length == note.text.length + 1 && value.selection.collapsed) {
            val caret = value.selection.min
            if (caret > 0 && value.text[caret - 1] == '\n') {
                val lineStart = lineStart(value.text, caret - 1)
                val previous = value.text.substring(lineStart, caret - 1)
                val continuation: String? = when {
                    previous.isBlank() -> null
                    previous.startsWith(BULLET_PREFIX) ->
                        if (previous.length == BULLET_PREFIX.length) null else BULLET_PREFIX
                    else -> NUMBERED_PREFIX.find(previous)?.let { match ->
                        if (match.value.length == previous.length) null
                        else "${match.groupValues[1].toInt() + 1}. "
                    }
                }
                if (continuation != null) {
                    val newText = value.text.substring(0, caret) + continuation + value.text.substring(caret)
                    value = TextFieldValue(newText, selection = TextRange(caret + continuation.length))
                }
            }
        }
        val newFormats = applyTextEdit(note.text, note.formats, value.text, activeFormat)
        note = note.copy(text = value.text, formats = newFormats)
        textFieldValue = value
        // L'utente ha scritto: da qui in poi il foglio segue il cursore.
        suppressCaretFollow = false
    }

    /**
     * Attiva o disattiva un elenco (puntato o numerato) sulle righe coperte
     * dalla selezione, o sulla riga del cursore se non c'e' selezione.
     * Se le righe portano gia' quel tipo di elenco, il marcatore viene tolto;
     * un eventuale elenco dell'altro tipo viene convertito.
     */
    fun toggleList(numbered: Boolean) {
        val text = note.text
        val selStart = textFieldValue.selection.min.coerceIn(0, text.length)
        val selEnd = textFieldValue.selection.max.coerceIn(0, text.length)
        val from = lineStart(text, selStart)
        val to = lineEnd(text, (selEnd - 1).coerceAtLeast(selStart))
        val lines = text.substring(from, to).split('\n')
        val markers = lines.map { listMarkerOf(it) }
        val nonBlank = markers.filterIndexed { i, _ -> lines[i].isNotBlank() }
        val allMarked = nonBlank.isNotEmpty() && nonBlank.all { it != null }
        val allThisKind = allMarked && nonBlank.all { it!!.numbered == numbered }

        // Un elenco numerato continua la numerazione della riga soprastante.
        var number = 1
        if (numbered && from > 0) {
            val aboveStart = lineStart(text, from - 1)
            val aboveLine = text.substring(aboveStart, lineEnd(text, aboveStart))
            NUMBERED_PREFIX.find(aboveLine)?.groupValues?.get(1)?.toIntOrNull()?.let { number = it + 1 }
        }

        val result = StringBuilder()
        var firstDelta = 0
        var totalDelta = 0
        lines.forEachIndexed { i, line ->
            val marker = markers[i]
            val delta: Int
            if (line.isBlank()) {
                result.append(line)
                delta = 0
            } else if (allThisKind) {
                // Toggle OFF: via il marcatore.
                result.append(line.substring(marker!!.prefixLength))
                delta = -marker.prefixLength
            } else {
                // Toggle ON: via un eventuale marcatore diverso e metti il nuovo.
                val stripped = if (marker != null) line.substring(marker.prefixLength) else line
                val prefix = if (numbered) "$number. " else BULLET_PREFIX
                if (numbered) number++
                result.append(prefix).append(stripped)
                delta = prefix.length - (marker?.prefixLength ?: 0)
            }
            if (i == 0) firstDelta = delta
            totalDelta += delta
            if (i < lines.size - 1) result.append('\n')
        }

        val newText = text.replaceRange(from, to, result.toString())
        onTextFieldChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(
                    (selStart + firstDelta).coerceIn(0, newText.length),
                    (selEnd + totalDelta).coerceIn(0, newText.length)
                )
            )
        )
    }

    /** Dimensione o colore: con selezione si applica alla selezione, altrimenti diventa formato attivo. */
    fun applyFormatChoice(kind: FormatKind, value: Long) {
        val selection = textFieldValue.selection
        if (selection.max > selection.min) {
            note = note.withFormatApplied(selection.min, selection.max, kind, value)
        }
        activeFormat = when (kind) {
            FormatKind.SIZE -> activeFormat.copy(textSizeSp = value.toInt())
            FormatKind.COLOR -> activeFormat.copy(color = value)
            else -> activeFormat
        }
    }

    /** Stile a interruttore (grassetto, corsivo, ...): ribalta lo stato corrente. */
    fun toggleStyle(kind: FormatKind) {
        val selection = textFieldValue.selection
        if (selection.max > selection.min) {
            // Il valore corrente si legge all'inizio della selezione, senza
            // ereditare dagli span che terminano esattamente li'.
            val current = note.formatValueAtSelectionStart(selection.min, kind) ?: 0L
            val newValue = if (current == 1L) 0L else 1L
            note = note.withFormatApplied(selection.min, selection.max, kind, newValue)
            activeFormat = activeFormat.toggled(kind, newValue == 1L)
        } else {
            val caret = selection.end.coerceIn(0, note.text.length)
            val current = activeFormat.toggleValue(kind)
                ?: (note.effectiveFormatValue(caret, kind) == 1L)
            activeFormat = activeFormat.toggled(kind, !current)
        }
    }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            onBack = { showSettings = false },
            onUpdate = ::updateSettings
        )
        return
    }

    if (showInfo) {
        InfoScreen(onBack = { showInfo = false })
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            SavedNotesSheet(
                notes = savedNotes,
                pinnedId = pinnedId,
                onCloseClick = { scope.launch { drawerState.close() } },
                onNoteClick = ::openSavedNote,
                onNotePin = ::pinNote,
                onNoteDelete = { noteToDelete = it },
                onNoteRename = { noteToRename = it },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    showSettings = true
                },
                onInfoClick = {
                    scope.launch { drawerState.close() }
                    showInfo = true
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                EditorTopBar(
                    mode = mode,
                    onMenuClick = {
                        // Aprendo il menu la tastiera non deve restare aperta.
                        keyboard?.hide()
                        scope.launch { drawerState.open() }
                    },
                    onTextModeClick = { mode = WritingMode.TEXT; eraserOn = false },
                    onHandModeClick = { mode = WritingMode.HAND },
                    onTrashClick = { showDiscardDialog = true },
                    onSaveClick = {
                        if (note.id == 0L) showTitleDialog = true else saveCurrentNote()
                    }
                )
            },
            bottomBar = {
                EditorBottomBar(
                    mode = mode,
                    note = note,
                    textFieldValue = textFieldValue,
                    activeFormat = activeFormat,
                    eraserOn = eraserOn,
                    onTextSizeSelected = { size -> applyFormatChoice(FormatKind.SIZE, size.toLong()) },
                    onStyleToggled = ::toggleStyle,
                    onListToggled = ::toggleList,
                    onTextColorSelected = { color -> applyFormatChoice(FormatKind.COLOR, color) },
                    onStrokeSizeSelected = { sizeDp -> note = note.copy(strokeSizeDp = sizeDp) },
                    onInkColorSelected = { color -> note = note.copy(inkColor = color) },
                    onEraserToggle = { eraserOn = !eraserOn }
                )
            }
        ) { innerPadding ->
            // Riserva della barra inferiore: quanto spazio occupa l'isola
            // degli strumenti dal fondo della finestra (barra + inset).
            val bottomReservePx =
                innerPadding.calculateBottomPadding().value * densityPxPerDp
            Box(
                modifier = Modifier
                    // Il foglio si estende fino in fondo, anche sotto la barra
                    // inferiore: si puo' scrivere nelle zone trasparenti ai
                    // lati dell'isola; l'isola stessa resta interattiva.
                    .padding(top = innerPadding.calculateTopPadding())
                    .fillMaxSize()
                    .onSizeChanged { viewportHeightPx = it.height }
            ) {
                // Su schermi larghi il foglio resta alla larghezza leggibile,
                // centrato invece di allargarsi a tutto schermo.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = MAX_CONTENT_WIDTH)
                        .align(Alignment.TopCenter)
                ) {
                    val sheetScroll = rememberScrollState()
                    // Foglio unico: la pagina intera scorre, cosi' il testo e
                    // i tratti a mano restano sempre sincronizzati.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(sheetScroll)
                    ) {
                        SheetArea(
                            mode = mode,
                            note = note,
                            textFieldValue = textFieldValue,
                            formats = note.formats,
                            focusRequester = textFocusRequester,
                            onValueChange = ::onTextFieldChange,
                            viewportHeightPx = viewportHeightPx,
                            bottomReservePx = bottomReservePx,
                            eraserOn = eraserOn,
                            eraseRadiusPx = eraseRadiusPx,
                            pxPerDp = densityPxPerDp,
                            sheetScroll = sheetScroll,
                            onEditorLayout = { textLayout = it },
                            onNoteChange = { note = it }
                        )
                    }

                    // Il cursore resta sempre in vista: se esce dalla finestra
                    // del foglio (sopra o dietro l'isola degli strumenti), lo
                    // scroll lo riporta a vista.
                    val density = LocalDensity.current
                    // Il testo parte a 12dp dal bordo del foglio.
                    val textInsetPx = with(density) { 12.dp.toPx() }
                    val linePx = with(density) { 22.sp.toPx() }
                    LaunchedEffect(
                        textFieldValue.selection.end,
                        textLayout,
                        viewportHeightPx,
                        bottomReservePx,
                        mode
                    ) {
                        val layout = textLayout ?: return@LaunchedEffect
                        if (mode != WritingMode.TEXT || viewportHeightPx <= 0) {
                            return@LaunchedEffect
                        }
                        if (suppressCaretFollow) {
                            // Finche' l'utente non scrive, il foglio resta
                            // dov'e': nessun salto al cursore. Il consumo
                            // avviene solo in onTextFieldChange.
                            return@LaunchedEffect
                        }
                        val caret =
                            textFieldValue.selection.end
                                .coerceIn(0, layout.layoutInput.text.length)
                        val rect = layout.getCursorRect(caret)
                        val lineTop = rect.top + textInsetPx
                        val lineBottom = rect.bottom + textInsetPx
                        val scrollTop = sheetScroll.value.toFloat()
                        // Limite inferiore visibile: la finestra del foglio
                        // finisce dove comincia l'isola della barra inferiore.
                        val visibleBottom = viewportHeightPx - bottomReservePx
                        // Con la tastiera aperta su schermi piccoli l'isola e la
                        // tastiera occupano quasi tutta la finestra: lo spazio
                        // visibile e' troppo poco per mostrarci una riga, e
                        // qualsiasi scroll nasconderebbe il cursore. Non si
                        // scorre affinche' non torni spazio utile.
                        if (visibleBottom < 2 * linePx) {
                            return@LaunchedEffect
                        }
                        val target = when {
                            // Cursore sopra la finestra: porta la riga in cima.
                            lineTop < scrollTop -> lineTop - linePx
                            // Cursore sotto la finestra: porta la riga sopra l'isola.
                            lineBottom > scrollTop + visibleBottom ->
                                lineBottom - visibleBottom + linePx
                            else -> return@LaunchedEffect
                        }
                        // Il foglio non puo' scorrere oltre la propria altezza;
                        // lo spazio extra sotto il testo e' lo stesso di SheetArea.
                        val extraPx = with(density) {
                            (24.dp.toPx() + linePx + bottomReservePx).toInt()
                        }
                        val maxScroll = with(density) {
                            max(viewportHeightPx, layout.size.height + extraPx) -
                                viewportHeightPx
                        }.coerceAtLeast(0)
                        val clamped = target.coerceIn(0f, maxScroll.toFloat())
                        if (clamped != scrollTop) {
                            sheetScroll.animateScrollTo(clamped.roundToInt())
                        }
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    discardCurrentNote()
                }) { Text(stringResource(R.string.discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Popup per il fissaggio senza widget: confermare aggiunge il widget
    // alla schermata home (dove supportato) e fissa la nota scelta.
    noteToPinWithoutWidget?.let { target ->
        AlertDialog(
            onDismissRequest = { noteToPinWithoutWidget = null },
            title = { Text(stringResource(R.string.widget_prompt_title)) },
            text = { Text(stringResource(R.string.widget_prompt_message)) },
            confirmButton = {
                TextButton(onClick = {
                    noteToPinWithoutWidget = null
                    pinnedId = target.id
                    pinnedStore.pin(target.id)
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            PinnedNoteWidget.updateAll(context)
                            // Aggiunta diretta del widget, dove il launcher la supporta.
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                runCatching {
                                    AppWidgetManager.getInstance(context).requestPinAppWidget(
                                        ComponentName(context, PinnedNoteWidget::class.java),
                                        null,
                                        null
                                    )
                                }
                            }
                        }
                    }
                }) { Text(stringResource(R.string.widget_prompt_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { noteToPinWithoutWidget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    noteToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, formatNoteDate(target.id))) },
            confirmButton = {
                TextButton(onClick = {
                    noteToDelete = null
                    // Se la nota eliminata era fissata, il widget torna vuoto.
                    val wasPinned = pinnedId == target.id
                    if (wasPinned) {
                        pinnedId = 0L
                        pinnedStore.unpin()
                    }
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            repository.deleteNote(target.id)
                            if (wasPinned) PinnedNoteWidget.updateAll(context)
                        }
                        savedNotes = withContext(Dispatchers.IO) { repository.listNotes() }
                    }
                }) { Text(stringResource(R.string.delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Rinomina del titolo dall'elenco: salva la nota stessa con il nuovo titolo.
    noteToRename?.let { target ->
        val handTitle = stringResource(R.string.default_hand_title)
        val emptyTitle = stringResource(R.string.empty_note_title)
        var titleText by remember(target.id) {
            mutableStateOf(
                target.title.ifBlank { noteDefaultTitle(target, handTitle, emptyTitle) }
            )
        }
        AlertDialog(
            onDismissRequest = { noteToRename = null },
            title = { Text(stringResource(R.string.rename_title)) },
            text = {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { if (it.length <= MAX_TITLE_LENGTH) titleText = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.title_hint)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    noteToRename = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            repository.saveNote(target.copy(title = titleText.trim()))
                            // Il titolo fissato nel widget cambia con la rinomina.
                            if (pinnedId == target.id) PinnedNoteWidget.updateAll(context)
                        }
                        savedNotes = withContext(Dispatchers.IO) { repository.listNotes() }
                    }
                }) { Text(stringResource(R.string.save_desc)) }
            },
            dismissButton = {
                TextButton(onClick = { noteToRename = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showTitleDialog) {
        val handTitle = stringResource(R.string.default_hand_title)
        val emptyTitle = stringResource(R.string.empty_note_title)
        var titleText by remember { mutableStateOf(noteDefaultTitle(note, handTitle, emptyTitle)) }
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text(stringResource(R.string.title_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.title_dialog_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { if (it.length <= MAX_TITLE_LENGTH) titleText = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.title_hint)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showTitleDialog = false
                    note = note.copy(title = titleText.trim())
                    saveCurrentNote()
                }) { Text(stringResource(R.string.save_desc)) }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Schermata delle impostazioni: modalita' predefinita, etichetta icona,
 * tema e lingua. Il cambio lingua chiede conferma e riavvia l'app,
 * perche' la nuova lingua entra in vigore all'avvio dell'activity.
 */
@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onUpdate: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    // Tasto indietro di sistema: torna all'editor invece di uscire dall'app.
    BackHandler { onBack() }
    // Lingua scelta ma da confermare: se impostata, mostra l'avviso di riavvio.
    var pendingLanguage by remember { mutableStateOf<LanguageSetting?>(null) }
    // Etichetta/colore icona scelti ma da confermare con riavvio.
    var pendingIconChange by remember { mutableStateOf<AppSettings?>(null) }
    fun askIconChange(updated: AppSettings) {
        if (updated.useShortLabel != settings.useShortLabel ||
            updated.iconColorKey != settings.iconColorKey
        ) {
            pendingIconChange = updated
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Su schermi larghi i contenuti restano alla larghezza leggibile, centrati.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = MAX_CONTENT_WIDTH)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.back_desc)
                    )
                }
                Text(
                    text = stringResource(R.string.settings_desc),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingsGroup(title = stringResource(R.string.settings_default_mode)) {
                    OptionChip(
                        labelRes = R.string.chip_text,
                        selected = settings.defaultMode == WritingMode.TEXT,
                        onClick = { onUpdate(settings.copy(defaultMode = WritingMode.TEXT)) }
                    )
                    OptionChip(
                        labelRes = R.string.chip_hand,
                        selected = settings.defaultMode == WritingMode.HAND,
                        onClick = { onUpdate(settings.copy(defaultMode = WritingMode.HAND)) }
                    )
                }
                SettingsGroup(title = stringResource(R.string.settings_language)) {
                    OptionChip(
                        labelRes = R.string.lang_it,
                        selected = settings.language == LanguageSetting.ITALIAN,
                        onClick = {
                            if (settings.language != LanguageSetting.ITALIAN) {
                                pendingLanguage = LanguageSetting.ITALIAN
                            }
                        }
                    )
                    OptionChip(
                        labelRes = R.string.lang_en,
                        selected = settings.language == LanguageSetting.ENGLISH,
                        onClick = {
                            if (settings.language != LanguageSetting.ENGLISH) {
                                pendingLanguage = LanguageSetting.ENGLISH
                            }
                        }
                    )
                    OptionChip(
                        labelRes = R.string.theme_system,
                        selected = settings.language == LanguageSetting.SYSTEM,
                        onClick = {
                            if (settings.language != LanguageSetting.SYSTEM) {
                                pendingLanguage = LanguageSetting.SYSTEM
                            }
                        }
                    )
                }
                SettingsGroup(title = stringResource(R.string.settings_theme)) {
                    OptionChip(
                        labelRes = R.string.theme_light,
                        selected = settings.theme == ThemeSetting.LIGHT,
                        onClick = { onUpdate(settings.copy(theme = ThemeSetting.LIGHT)) }
                    )
                    OptionChip(
                        labelRes = R.string.theme_dark,
                        selected = settings.theme == ThemeSetting.DARK,
                        onClick = { onUpdate(settings.copy(theme = ThemeSetting.DARK)) }
                    )
                    OptionChip(
                        labelRes = R.string.theme_system,
                        selected = settings.theme == ThemeSetting.SYSTEM,
                        onClick = { onUpdate(settings.copy(theme = ThemeSetting.SYSTEM)) }
                    )
                }
                // Personalizza icona: etichetta nel drawer e sfondo dell'icona.
                Column {
                    Text(
                        text = stringResource(R.string.settings_icon_customize),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_icon_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OptionChip(
                            labelRes = R.string.note_label,
                            selected = !settings.useShortLabel,
                            onClick = { askIconChange(settings.copy(useShortLabel = false)) }
                        )
                        OptionChip(
                            labelRes = R.string.app_name,
                            selected = settings.useShortLabel,
                            onClick = { askIconChange(settings.copy(useShortLabel = true)) }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.settings_icon_color),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Selettore come quello dell'inchiostro: un pallino singolo
                    // che mostra il colore scelto; il popup porta la tavolozza.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconColorPicker(
                            currentKey = settings.iconColorKey,
                            onPick = { key -> askIconChange(settings.copy(iconColorKey = key)) }
                        )
                        // Ripristina: torna al blu del logo.
                        OptionChip(
                            labelRes = R.string.settings_icon_color_reset,
                            selected = settings.iconColorKey == "default",
                            onClick = { askIconChange(settings.copy(iconColorKey = "default")) }
                        )
                    }
                }
            }
            }
        }
    }

    // Conferma del cambio lingua: salva la scelta e riavvia l'applicazione.
    pendingLanguage?.let { chosen ->
        AlertDialog(
            onDismissRequest = { pendingLanguage = null },
            title = { Text(stringResource(R.string.language_restart_title)) },
            text = { Text(stringResource(R.string.language_restart_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingLanguage = null
                    onUpdate(settings.copy(language = chosen))
                    restartApp(context)
                }) { Text(stringResource(R.string.language_restart_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingLanguage = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Conferma del cambio icona: salva etichetta e colore e riavvia l'app.
    pendingIconChange?.let { updated ->
        AlertDialog(
            onDismissRequest = { pendingIconChange = null },
            title = { Text(stringResource(R.string.icon_restart_title)) },
            text = { Text(stringResource(R.string.icon_restart_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingIconChange = null
                    onUpdate(updated)
                    restartApp(context)
                }) { Text(stringResource(R.string.icon_restart_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingIconChange = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Riavvio forzato dell'app: l'intent di lancio sostituisce il task corrente,
 * cosi' l'activity viene ricreata da zero e la nuova lingua entra in vigore.
 */
private fun restartApp(context: Context) {
    var current: Context = context
    while (current is ContextWrapper) {
        if (current is Activity) break
        current = current.baseContext
    }
    context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        context.startActivity(intent)
    }
    (current as? Activity)?.finishAffinity()
    Runtime.getRuntime().exit(0)
}

@Composable
private fun SettingsGroup(title: String, content: @Composable RowScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

/** Chip segmentato compatto: ocupa poco e sta sempre in riga. */
@Composable
private fun OptionChip(labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        onClick = onClick
    ) {
        Text(
            text = stringResource(labelRes),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Barra superiore: pillola arrotondata flottante con posizioni fisse.
 * Hamburger e selettore di modalita' a sinistra, salva e cestino
 * accoppiati all'estrema destra. Gli strumenti di formattazione
 * vivono nella barra inferiore, per modalita'.
 */
@Composable
private fun EditorTopBar(
    mode: WritingMode,
    onMenuClick: () -> Unit,
    onTextModeClick: () -> Unit,
    onHandModeClick: () -> Unit,
    onTrashClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(start = 2.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BarIcon(R.drawable.ic_menu, R.string.menu_desc, onMenuClick)
                Spacer(Modifier.width(2.dp))
                WritingModeToggle(
                    mode = mode,
                    onTextModeClick = onTextModeClick,
                    onHandModeClick = onHandModeClick
                )
                Spacer(Modifier.weight(1f))
                BarIcon(
                    iconRes = R.drawable.ic_save,
                    descRes = R.string.save_desc,
                    onClick = onSaveClick,
                    // Target di tocco 44dp: su un piccolo schermo un pulsante
                    // troppo piccolo e' facile da mancare.
                    buttonSize = 44.dp,
                    iconSize = 24.dp
                )
                BarIcon(
                    iconRes = R.drawable.ic_delete,
                    descRes = R.string.trash_desc,
                    onClick = onTrashClick,
                    buttonSize = 44.dp,
                    iconSize = 24.dp
                )
            }
        }
    }
}

/**
 * Barra strumenti inferiore: pillola arrotondata flottante, gli strumenti
 * cambiano con la modalita'. In modalita' testo: dimensione, stile e colore
 * del testo. In modalita' a mano: dimensione del tratto, gomma e colore.
 */
@Composable
private fun EditorBottomBar(
    mode: WritingMode,
    note: NoteData,
    textFieldValue: TextFieldValue,
    activeFormat: FormatStyle,
    eraserOn: Boolean,
    onTextSizeSelected: (Int) -> Unit,
    onStyleToggled: (FormatKind) -> Unit,
    onListToggled: (Boolean) -> Unit,
    onTextColorSelected: (Long) -> Unit,
    onStrokeSizeSelected: (Float) -> Unit,
    onInkColorSelected: (Long) -> Unit,
    onEraserToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // La barra resta sempre visibile sopra la tastiera: si usa il
            // piu' grande tra l'inset della tastiera e quello della barra
            // di navigazione di sistema.
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(52.dp)
                    .padding(start = 6.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (mode) {
                    WritingMode.TEXT -> {
                        TextSizeButton(
                            currentSizeSp = activeFormat.textSizeSp ?: 16,
                            onSizeSelected = onTextSizeSelected
                        )
                        ToolButtonDivider()
                        StyleToolButton(
                            note = note,
                            textFieldValue = textFieldValue,
                            activeFormat = activeFormat,
                            onToggle = onStyleToggled
                        )
                        ToolButtonDivider()
                        ListButton(
                            note = note,
                            textFieldValue = textFieldValue,
                            onListToggled = onListToggled
                        )
                        ToolButtonDivider()
                        ColorToolButton(
                            current = activeFormat.color ?: 0L,
                            defaultColor = MaterialTheme.colorScheme.onSurface,
                            onPick = onTextColorSelected
                        )
                    }

                    WritingMode.HAND -> {
                        StrokeSizeButton(
                            currentDp = note.strokeSizeDp,
                            onSelected = onStrokeSizeSelected
                        )
                        ToolButtonDivider()
                        BarIcon(
                            iconRes = R.drawable.ic_eraser,
                            descRes = R.string.eraser_desc,
                            selected = eraserOn,
                            onClick = onEraserToggle
                        )
                        ToolButtonDivider()
                        ColorToolButton(
                            current = note.inkColor,
                            defaultColor = MaterialTheme.colorScheme.primary,
                            onPick = onInkColorSelected
                        )
                    }
                }
            }
        }
    }
}

/** Separatore sottile tra gli strumenti della barra inferiore. */
@Composable
private fun ToolButtonDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(width = 1.dp, height = 22.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun BarIcon(
    iconRes: Int,
    descRes: Int,
    onClick: () -> Unit,
    selected: Boolean = false,
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    IconButton(onClick = onClick, modifier = Modifier.size(buttonSize)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(descRes),
            modifier = Modifier.size(iconSize),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Interruttore compatto tra scrittura con tastiera e scrittura a mano. */
@Composable
private fun WritingModeToggle(
    mode: WritingMode,
    onTextModeClick: () -> Unit,
    onHandModeClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(2.dp)
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            ToggleIcon(R.drawable.ic_keyboard, R.string.mode_text_desc, mode == WritingMode.TEXT, onTextModeClick)
            ToggleIcon(R.drawable.ic_pen, R.string.mode_hand_desc, mode == WritingMode.HAND, onHandModeClick)
        }
    }
}

@Composable
private fun ToggleIcon(iconRes: Int, descRes: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(descRes),
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ---------- Strumenti per il testo ----------

/** Pulsante "Aa": menu con le dimensioni del testo. */
@Composable
private fun TextSizeButton(currentSizeSp: Int, onSizeSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_text_size),
            contentDescription = stringResource(R.string.size_text_desc),
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        TEXT_SIZES.forEach { size ->
            DropdownMenuItem(
                // Solo il numero, disegnato alla dimensione che rappresentera'.
                text = { Text(text = size.toString(), fontSize = size.sp) },
                trailingIcon = { if (currentSizeSp == size) CheckIcon() },
                onClick = {
                    onSizeSelected(size)
                    expanded = false
                }
            )
        }
    }
}

/** Pulsante "B": menu con gli stili del testo, a interruttore. */
@Composable
private fun StyleToolButton(
    note: NoteData,
    textFieldValue: TextFieldValue,
    activeFormat: FormatStyle,
    onToggle: (FormatKind) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_text_style),
            contentDescription = stringResource(R.string.style_desc),
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
    val caret = textFieldValue.selection.end.coerceIn(0, note.text.length)
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        listOf(
            FormatKind.BOLD to R.string.style_bold,
            FormatKind.ITALIC to R.string.style_italic,
            FormatKind.UNDERLINE to R.string.style_underline,
            FormatKind.STRIKE to R.string.style_strike
        ).forEach { (kind, labelRes) ->
            val checked = activeFormat.toggleValue(kind)
                ?: (note.effectiveFormatValue(caret, kind) == 1L)
            // La didascalia mostra l'effetto che applica: grassetto in grassetto, ecc.
            val captionStyle = when (kind) {
                FormatKind.BOLD -> TextStyle(fontWeight = FontWeight.Bold)
                FormatKind.ITALIC -> TextStyle(fontStyle = FontStyle.Italic)
                FormatKind.UNDERLINE -> TextStyle(textDecoration = TextDecoration.Underline)
                else -> TextStyle(textDecoration = TextDecoration.LineThrough)
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(labelRes), style = captionStyle) },
                trailingIcon = { if (checked) CheckIcon() },
                onClick = { onToggle(kind) }
            )
        }
    }
}

/**
 * Pulsante unico del selettore colori: mostra un cerchio del colore scelto
 * (per il "predefinito" usa il colore dell'inchiostro del tema). Al tap apre
 * un popup — come quelli di dimensione e stile — che pero' presenta tutti
 * i colori insieme in due righe di cerchietti, senza elenco.
 */
@Composable
private fun ColorToolButton(current: Long, defaultColor: Color, onPick: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
        ColorDot(
            color = if (current == 0L) defaultColor else Color(current),
            selected = current == 0L,
            isDefault = true
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorDot(
                    color = defaultColor,
                    selected = current == 0L,
                    isDefault = true,
                    onClick = {
                        onPick(0L)
                        expanded = false
                    }
                )
                NOTE_COLORS.take(5).forEach { option ->
                    ColorDot(
                        color = Color(option.value),
                        selected = current == option.value,
                        onClick = {
                            onPick(option.value)
                            expanded = false
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NOTE_COLORS.drop(5).forEach { option ->
                    ColorDot(
                        color = Color(option.value),
                        selected = current == option.value,
                        onClick = {
                            onPick(option.value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Selettore del colore dell'icona: un pallino singolo che mostra il colore
 * impostato; al tap apre un popup con la tavolozza, come nell'editor.
 */
@Composable
private fun IconColorPicker(currentKey: String, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = SettingsRepository.ICON_COLOR_OPTIONS
    val current = options.firstOrNull { it.key == currentKey } ?: options.first()
    IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
        ColorDot(
            color = Color(current.argb),
            selected = current.key == "default",
            isDefault = true
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.take(6).forEach { option ->
                    ColorDot(
                        color = Color(option.argb),
                        selected = option.key == currentKey,
                        isDefault = option.key == "default",
                        onClick = {
                            onPick(option.key)
                            expanded = false
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.drop(6).forEach { option ->
                    ColorDot(
                        color = Color(option.argb),
                        selected = option.key == currentKey,
                        isDefault = option.key == "default",
                        onClick = {
                            onPick(option.key)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/** Cerchio di colore; se selezionato porta un anello del colore primario. */
@Composable
private fun ColorDot(
    color: Color,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    isDefault: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(if (selected) 22.dp else 18.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else Modifier
            )
            .then(
                // Il "predefinito" ha un bordo tenue per distinguersi dagli altri.
                if (isDefault) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                } else Modifier
            )
    )
}

// ---------- Strumenti per la scrittura a mano ----------

/** Pulsante dei pallini: menu con lo spessore del tratto (vale anche per la gomma). */
@Composable
private fun StrokeSizeButton(currentDp: Float, onSelected: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_stroke_size),
            contentDescription = stringResource(R.string.size_stroke_desc),
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        STROKE_SIZES.forEach { option ->
            DropdownMenuItem(
                // Solo illustrazioni: una linea ondulata con lo spessore reale.
                text = {},
                leadingIcon = { StrokeSample(widthDp = option.valueDp) },
                trailingIcon = { if (currentDp == option.valueDp) CheckIcon() },
                onClick = {
                    onSelected(option.valueDp)
                    expanded = false
                }
            )
        }
    }
}

/** Illustrazione dello spessore: un'onda disegnata con quello spessore reale. */
@Composable
private fun StrokeSample(widthDp: Float) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = Modifier.size(width = 40.dp, height = 14.dp)) {
        val thickness = widthDp.dp.toPx().coerceIn(1f, size.height)
        val y = size.height / 2f
        val amp = size.height * 0.28f
        val path = Path().apply {
            moveTo(0f, y)
            quadraticBezierTo(size.width * 0.25f, y - amp, size.width * 0.5f, y)
            quadraticBezierTo(size.width * 0.75f, y + amp, size.width, y)
        }
        drawPath(
            path = path,
            color = color,
            style = DrawStroke(width = thickness, cap = StrokeCap.Round)
        )
    }
}

/** Pulsante "elenco": menu con elenco puntato e numerato. */
@Composable
private fun ListButton(
    note: NoteData,
    textFieldValue: TextFieldValue,
    onListToggled: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_list),
            contentDescription = stringResource(R.string.list_desc),
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_bullet)) },
            leadingIcon = { Text("•") },
            trailingIcon = {
                if (selectionMarked(note.text, textFieldValue.selection, false)) CheckIcon()
            },
            onClick = {
                onListToggled(false)
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_numbered)) },
            leadingIcon = { Text("1.") },
            trailingIcon = {
                if (selectionMarked(note.text, textFieldValue.selection, true)) CheckIcon()
            },
            onClick = {
                onListToggled(true)
                expanded = false
            }
        )
    }
}

@Composable
private fun CheckIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_check),
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}

// ---------- Aree di scrittura ----------

/**
 * Foglio unico: testo e tratti a mano condividono la stessa pagina, che e'
 * alta almeno quanto lo schermo e cresce col testo. Il foglio intero scorre
 * fra le barre, cosi' testo e disegno restano sempre sincronizzati.
 */
@Composable
private fun SheetArea(
    mode: WritingMode,
    note: NoteData,
    textFieldValue: TextFieldValue,
    formats: List<FormatSpan>,
    focusRequester: FocusRequester,
    onValueChange: (TextFieldValue) -> Unit,
    viewportHeightPx: Int,
    bottomReservePx: Float,
    eraserOn: Boolean,
    eraseRadiusPx: Float,
    pxPerDp: Float,
    sheetScroll: ScrollState,
    onEditorLayout: (TextLayoutResult) -> Unit,
    onNoteChange: (NoteData) -> Unit
) {
    // Altezza intrinseca del testo, misurata dall'impaginazione: evita di
    // misurare nodi con larghezza limitata (resterebbero bloccati).
    var textHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    // Sotto l'ultima riga: padding del foglio (12+12dp), una riga di riserva
    // per assorbire il ritardo della misurazione e lo spazio dell'isola della
    // barra inferiore, cosi' l'ultima riga puo' risalire sopra l'isola.
    val extraPx = with(density) {
        (24.dp.toPx() + 22.sp.toPx() + bottomReservePx).toInt()
    }
    val sheetHeightPx = max(viewportHeightPx, textHeightPx + extraPx).coerceAtLeast(1)
    val sheetHeight = with(density) { sheetHeightPx.toDp() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetHeight)
    ) {
        // Testo: sempre visibile; modificabile solo in modalita' testo.
        Crossfade(targetState = mode, label = "testo") { currentMode ->
            if (currentMode == WritingMode.TEXT) {
                TextEditorArea(
                    value = textFieldValue,
                    formats = formats,
                    focusRequester = focusRequester,
                    minContentPx = sheetHeightPx,
                    onTextHeight = { textHeightPx = it },
                    onLayout = onEditorLayout,
                    onValueChange = onValueChange
                )
            } else {
                TextDisplayArea(
                    note = note,
                    minContentPx = sheetHeightPx,
                    onTextHeight = { textHeightPx = it }
                )
            }
        }
        // Tratti: sempre disegnati sopra il testo; toccabili solo in modalita' a mano.
        HandwritingArea(
            note = note,
            interactive = mode == WritingMode.HAND,
            eraserOn = eraserOn,
            eraseRadiusPx = eraseRadiusPx,
            pxPerDp = pxPerDp,
            sheetScroll = sheetScroll,
            onNoteChange = onNoteChange
        )
    }
}

/** Pagina di testo: campo largo quanto il foglio, gia' pronto a ricevere il focus. */
@Composable
private fun TextEditorArea(
    value: TextFieldValue,
    formats: List<FormatSpan>,
    focusRequester: FocusRequester,
    minContentPx: Int,
    onTextHeight: (Int) -> Unit,
    onLayout: (TextLayoutResult) -> Unit,
    onValueChange: (TextFieldValue) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = with(LocalDensity.current) { minContentPx.toDp() })
            .padding(12.dp)
    ) {
        if (value.text.isEmpty()) {
            Text(
                text = stringResource(R.string.write_hint),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            // L'altezza del testo definita dall'impaginazione: cresce col
            // contenuto e fa crescere il foglio con lui. Il layout serve
            // anche a far seguire il cursore allo scroll del foglio.
            onTextLayout = { result ->
                onTextHeight(result.size.height)
                onLayout(result)
            },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            // La formattazione a intervalli e' resa con una trasformazione visuale.
            visualTransformation = VisualTransformation { raw ->
                TransformedText(raw.text.annotated(formats), OffsetMapping.Identity)
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}

/** Testo in sola lettura, con la stessa formattazione, in modalita' a mano. */
@Composable
private fun TextDisplayArea(
    note: NoteData,
    minContentPx: Int,
    onTextHeight: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = with(LocalDensity.current) { minContentPx.toDp() })
            .padding(12.dp)
    ) {
        Text(
            text = note.text.annotated(note.formats),
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            onTextLayout = { onTextHeight(it.size.height) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Pagina a mano: ogni tratto mantiene il proprio spessore e colore. */
@Composable
private fun HandwritingArea(
    note: NoteData,
    interactive: Boolean,
    eraserOn: Boolean,
    eraseRadiusPx: Float,
    pxPerDp: Float,
    sheetScroll: ScrollState,
    onNoteChange: (NoteData) -> Unit
) {
    val strokes = remember(note.strokes) {
        note.strokes.map { InkStroke(it.points.strokeToPoints(), it.widthDp.dp, it.color) }
    }
    // Larghezza del canvas: salvata con i tratti, serve al widget per
    // ricomporre la pagina esattamente com'e' nell'editor.
    var canvasWidthPx by remember { mutableStateOf(0f) }
    HandwritingCanvas(
        strokes = strokes,
        interactive = interactive,
        eraserEnabled = eraserOn,
        defaultColor = MaterialTheme.colorScheme.primary,
        currentColor = note.inkColor,
        currentWidth = note.strokeSizeDp.dp,
        scroll = sheetScroll,
        onStrokeFinished = { ink ->
            onNoteChange(
                note.copy(
                    strokes = note.strokes + Stroke(
                        points = ink.points.strokeToFlat(),
                        widthDp = ink.width.value,
                        color = ink.color
                    ),
                    canvasWidthPx = if (canvasWidthPx > 0f) canvasWidthPx else note.canvasWidthPx
                )
            )
        },
        onErase = { from, to ->
            onNoteChange(
                note.copy(
                    strokes = eraseFromStrokes(note.strokes, from, to, eraseRadiusPx, pxPerDp)
                )
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasWidthPx = it.width.toFloat() }
    )
}

/** Menu laterale con l'elenco delle note salvate. */
@Composable
private fun SavedNotesSheet(
    notes: List<NoteData>,
    pinnedId: Long,
    onCloseClick: () -> Unit,
    onNoteClick: (NoteData) -> Unit,
    onNotePin: (NoteData) -> Unit,
    onNoteDelete: (NoteData) -> Unit,
    onNoteRename: (NoteData) -> Unit,
    onSettingsClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    // Larghezza frazionaria con tetto: su schermi piccoli resta visibile una
    // parte dell'editor, che si puo' toccare (o scorrere verso sinistra) per
    // chiudere; su schermi larghi il drawer non ingloba tutto lo schermo.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val drawerMaxWidth = (screenWidthDp * 0.72f).dp.coerceAtMost(320.dp)
    ModalDrawerSheet(
        modifier = Modifier.width(drawerMaxWidth)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.saved_notes_title),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
            IconButton(onClick = onCloseClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_desc)
                )
            }
        }
        HorizontalDivider()

        // Elenco scorrevole, con barra di scorrimento sempre visibile.
        val listState = rememberLazyListState()
        val layoutInfo = listState.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (notes.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_list),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(notes, key = { it.id }) { note ->
                        SavedNoteRow(
                            note = note,
                            isPinned = pinnedId == note.id,
                            onNoteClick = onNoteClick,
                            onNotePin = onNotePin,
                            onNoteDelete = onNoteDelete,
                            onNoteRename = onNoteRename
                        )
                        HorizontalDivider()
                    }
                }
                if (totalItems > 0 && visibleItems.isNotEmpty() && totalItems > visibleItems.size) {
                    val firstVisible = visibleItems.first().index
                    val thumbTop = maxHeight * (firstVisible.toFloat() / totalItems)
                    val thumbHeight = maxHeight * (visibleItems.size.toFloat() / totalItems).coerceIn(0.15f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = thumbTop)
                            .padding(end = 3.dp)
                            .size(width = 3.dp, height = thumbHeight)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                CircleShape
                            )
                    )
                }
            }
        }

        HorizontalDivider()
        // Sezione fissa in basso, sempre visibile: due azioni affiancate.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            DrawerActionCell(
                iconRes = R.drawable.ic_settings,
                labelRes = R.string.settings_desc,
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f)
            )
            DrawerActionCell(
                iconRes = R.drawable.ic_info,
                labelRes = R.string.info_desc,
                onClick = onInfoClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Cella del menu: icona sopra l'etichetta, compatta per lo schermo piccolo. */
@Composable
private fun DrawerActionCell(
    iconRes: Int,
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(labelRes),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SavedNoteRow(
    note: NoteData,
    isPinned: Boolean,
    onNoteClick: (NoteData) -> Unit,
    onNotePin: (NoteData) -> Unit,
    onNoteDelete: (NoteData) -> Unit,
    onNoteRename: (NoteData) -> Unit
) {
    val handTitle = stringResource(R.string.default_hand_title)
    val emptyTitle = stringResource(R.string.empty_note_title)
    val title = noteDefaultTitle(note, handTitle, emptyTitle)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNoteClick(note) }
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Data e ora su due righe, piu' piccole: cosi' restano spazio
            // per i pulsanti anche sullo schermo stretto.
            Column(
                modifier = Modifier.padding(top = 1.dp)
            ) {
                Text(
                    text = formatNoteDay(note.id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatNoteTime(note.id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row {
            IconButton(
                onClick = { onNotePin(note) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_pin),
                    contentDescription = stringResource(
                        if (isPinned) R.string.unpin_desc else R.string.pin_desc
                    ),
                    modifier = Modifier.size(22.dp),
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onNoteRename(note) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.rename_desc),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                onClick = { onNoteDelete(note) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete_desc),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun formatNoteDate(id: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(id))

private fun formatNoteDay(id: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(id))

private fun formatNoteTime(id: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(id))

/** Titolo mostrato per una nota: quello scelto, o un suggerimento dal contenuto. */
private fun noteDefaultTitle(note: NoteData, handTitle: String, emptyTitle: String): String =
    when {
        note.title.isNotBlank() -> note.title
        note.text.isNotBlank() -> note.text.trim().lines().first().take(MAX_TITLE_LENGTH).trim()
        note.strokes.isNotEmpty() -> handTitle
        else -> emptyTitle
    }
