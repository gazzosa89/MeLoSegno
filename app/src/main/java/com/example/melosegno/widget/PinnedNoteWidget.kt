package com.example.melosegno.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.RemoteViews
import com.example.melosegno.MainActivity
import com.example.melosegno.R
import com.example.melosegno.data.FormatKind
import com.example.melosegno.data.NoteData
import com.example.melosegno.data.NoteRepository
import com.example.melosegno.data.PinnedNoteStore

/**
 * Widget della nota fissata: mostra titolo e contenuto. Con solo testo o solo
 * disegno riempie lo spazio; con entrambi ricostruisce la pagina intera
 * com'e' nell'editor (testo impaginato e tratti sovrapposti).
 * Toccando il widget l'app si apre direttamente su quella nota.
 * Il contenuto viene aggiornato dall'app a ogni salvataggio, fissaggio,
 * toglio di fissaggio, rinomina o eliminazione della nota fissata.
 */
class PinnedNoteWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateWidgets(context, manager, ids)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        // Il widget si adatta alle dimensioni scelte dall'utente.
        updateWidgets(context, manager, intArrayOf(appWidgetId))
    }

    companion object {

        const val EXTRA_NOTE_ID = "com.example.melosegno.widget.EXTRA_NOTE_ID"

        private const val FALLBACK_WIDTH_PX = 360
        private const val FALLBACK_HEIGHT_PX = 260
        private const val BITMAP_PADDING_PX = 12f
        // Limite di ingrandimento: un disegno minuscolo non deve riempire tutto.
        private const val MAX_SCALE = 4f

        /** Aggiorna tutti i widget presenti nella schermata home. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PinnedNoteWidget::class.java))
            if (ids.isNotEmpty()) updateWidgets(context, manager, ids)
        }

        private fun updateWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val noteId = PinnedNoteStore(context).pinnedNoteId()
            val note = if (noteId != 0L) NoteRepository(context).loadNote(noteId) else null
            for (id in ids) {
                manager.updateAppWidget(id, buildRemoteViews(context, manager, id, note))
            }
        }

        private fun buildRemoteViews(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
            note: NoteData?
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.pinned_note_widget)

            if (note == null) {
                views.setViewVisibility(R.id.widget_strokes, View.GONE)
                views.setViewVisibility(R.id.widget_text, View.GONE)
                views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                views.setTextViewText(R.id.widget_title, "")
                return views
            }

            // Tap sul widget: apre l'app con la nota fissata gia' caricata.
            val launch = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, note.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context,
                    note.id.toInt(),
                    launch,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            views.setTextViewText(R.id.widget_title, widgetTitle(context, note))

            val hasStrokes = note.strokes.isNotEmpty()
            val hasText = note.text.isNotBlank()
            val density = context.resources.displayMetrics.density
            // Le misure del testo sono in sp: seguono lo scale dei font di
            // sistema, come nell'editor.
            val spPx = density * context.resources.configuration.fontScale
            when {
                // Testo e disegno insieme: la pagina intera com'e' nell'editor,
                // in un'unica immagine (testo impaginato e tratti sovrapposti).
                hasStrokes && hasText -> {
                    views.setViewVisibility(R.id.widget_strokes, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_text, View.GONE)
                    views.setViewVisibility(R.id.widget_empty, View.GONE)
                    val size = widgetSizePx(context, manager, appWidgetId)
                    val composite = renderNoteBitmap(context, note, size.first, size.second)
                    if (composite != null) {
                        views.setImageViewBitmap(R.id.widget_strokes, composite)
                    } else {
                        // Impossibile comporre la pagina: disegno sopra, testo sotto.
                        views.setViewVisibility(R.id.widget_text, View.VISIBLE)
                        views.setTextViewText(R.id.widget_text, styledText(note, spPx))
                    }
                }
                hasStrokes -> {
                    views.setViewVisibility(R.id.widget_strokes, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_text, View.GONE)
                    views.setViewVisibility(R.id.widget_empty, View.GONE)
                    val size = widgetSizePx(context, manager, appWidgetId)
                    renderStrokesBitmap(
                        context, note, size.first, size.second, density
                    )?.let {
                        views.setImageViewBitmap(R.id.widget_strokes, it)
                    }
                }
                hasText -> {
                    views.setViewVisibility(R.id.widget_strokes, View.GONE)
                    views.setViewVisibility(R.id.widget_text, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_empty, View.GONE)
                    views.setTextViewText(R.id.widget_text, styledText(note, spPx))
                }
                else -> {
                    views.setViewVisibility(R.id.widget_strokes, View.GONE)
                    views.setViewVisibility(R.id.widget_text, View.GONE)
                    views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
                    views.setTextViewText(R.id.widget_empty, context.getString(R.string.widget_empty_note))
                }
            }
            return views
        }

        /** Titolo scelto, o un suggerimento dal contenuto, come nell'elenco delle note. */
        private fun widgetTitle(context: Context, note: NoteData): String = when {
            note.title.isNotBlank() -> note.title
            note.text.isNotBlank() -> note.text.trim().lines().first().take(40).trim()
            note.strokes.isNotEmpty() -> context.getString(R.string.default_hand_title)
            else -> context.getString(R.string.empty_note_title)
        }

        /** Dimensione corrente del widget in pixel, per rendere il disegno nitido. */
        private fun widgetSizePx(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ): Pair<Int, Int> {
            val options = manager.getAppWidgetOptions(appWidgetId)
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            val density = context.resources.displayMetrics.density
            val width = if (widthDp > 0) (widthDp * density).toInt().coerceAtLeast(64) else FALLBACK_WIDTH_PX
            val height = if (heightDp > 0) (heightDp * density).toInt().coerceAtLeast(64) else FALLBACK_HEIGHT_PX
            return width to height
        }

        /**
         * Rende i tratti come bitmap: la geometria viene scalata per entrare
         * nello spazio disponibile, spessori e colori scalati con la stessa
         * proporzione.
         */
        private fun renderStrokesBitmap(
            context: Context,
            note: NoteData,
            width: Int,
            height: Int,
            density: Float
        ): Bitmap? {
            val strokes = note.strokes
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            strokes.forEach { stroke ->
                val points = stroke.points
                var i = 0
                while (i + 1 < points.size) {
                    minX = minOf(minX, points[i]); maxX = maxOf(maxX, points[i])
                    minY = minOf(minY, points[i + 1]); maxY = maxOf(maxY, points[i + 1])
                    i += 2
                }
            }
            if (maxX < minX || maxY < minY) return null
            val boxWidth = (maxX - minX).coerceAtLeast(1f)
            val boxHeight = (maxY - minY).coerceAtLeast(1f)
            val usableWidth = width - 2 * BITMAP_PADDING_PX
            val usableHeight = height - 2 * BITMAP_PADDING_PX
            if (usableWidth <= 0f || usableHeight <= 0f) return null
            val scale = minOf(
                usableWidth / boxWidth,
                usableHeight / boxHeight,
                MAX_SCALE
            )
            val offsetX = BITMAP_PADDING_PX + (usableWidth - boxWidth * scale) / 2f
            val offsetY = BITMAP_PADDING_PX + (usableHeight - boxHeight * scale) / 2f

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            strokes.forEach { stroke ->
                // Colore salvato sul tratto; se assente si usa quello della
                // nota, e se manca anche quello l'inchiostro del tema corrente
                // del widget: scuro su carta chiara, chiaro su carta scura
                // (valore night di widget_text). Cosi' i tratti con il colore
                // predefinito restano leggibili anche col tema scuro.
                paint.color = (if (stroke.color != 0L) stroke.color else note.inkColor)
                    .takeIf { it != 0L }?.toInt()
                    ?: context.getColor(R.color.widget_text)
                // Lo spessore segue la densita' del device, come nell'editor.
                paint.strokeWidth = (stroke.widthDp * density * scale).coerceAtLeast(2f)
                val points = stroke.points
                if (points.size >= 4) {
                    val path = android.graphics.Path()
                    path.moveTo(points[0] * scale + offsetX, points[1] * scale + offsetY)
                    var i = 2
                    while (i + 1 < points.size) {
                        path.lineTo(points[i] * scale + offsetX, points[i + 1] * scale + offsetY)
                        i += 2
                    }
                    canvas.drawPath(path, paint)
                } else if (points.size == 2) {
                    canvas.drawPoint(points[0] * scale + offsetX, points[1] * scale + offsetY, paint)
                }
            }
            return bitmap
        }

        /**
         * Rende la pagina intera com'e' nell'editor: testo impaginato con lo
         * stesso rientro e le stesse misure (16sp, interlinea 22sp) e tratti
         * sovrapposti nelle loro posizioni originali, tutto in un'unica
         * immagine scalata per entrare nel widget.
         *
         * La larghezza di riferimento e' quella del canvas dell'editor, salvata
         * nella nota al momento del disegno; per le note piu' vecchie si stima
         * la larghezza dello schermo (nota e widget stanno sullo stesso telefono).
         */
        private fun renderNoteBitmap(
            context: Context,
            note: NoteData,
            widgetWidth: Int,
            widgetHeight: Int
        ): Bitmap? {
            val density = context.resources.displayMetrics.density
            val canvasWidth = if (note.canvasWidthPx > 0f) note.canvasWidthPx
            else context.resources.displayMetrics.widthPixels.toFloat()
            if (canvasWidth <= 0f) return null
            val padding = 12f * density
            val textWidth = (canvasWidth - 2f * padding).toInt().coerceAtLeast(1)

            // Testo impaginato con le misure dell'editor: come l'app, anche il
            // bitmap segue lo scale dei font di sistema (le misure sono in sp).
            val spPx = density * context.resources.configuration.fontScale
            val textPaint = TextPaint().apply {
                textSize = 16f * spPx
                color = context.getColor(R.color.widget_text)
                isAntiAlias = true
            }
            val metrics = textPaint.fontMetrics
            val lineExtra = 22f * spPx - (metrics.descent - metrics.ascent)
            val spannable = styledText(note, spPx)
            val textLayout = StaticLayout.Builder
                .obtain(spannable, 0, spannable.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(lineExtra, 1f)
                .setIncludePad(false)
                .build()

            // Bordo inferiore dell'inchiostro: y massima dei punti piu' mezzo spessore.
            var strokesBottom = 0f
            note.strokes.forEach { stroke ->
                val half = stroke.widthDp * density / 2f
                var i = 1
                while (i < stroke.points.size) {
                    strokesBottom = maxOf(strokesBottom, stroke.points[i] + half)
                    i += 2
                }
            }
            val contentHeight = maxOf(padding + textLayout.height, strokesBottom) + padding

            // Scala per entrare nel widget; una nota minuscola puo' ingrandirsi.
            val scale = minOf(widgetWidth / canvasWidth, widgetHeight / contentHeight)
                .coerceIn(0f, MAX_SCALE)
            if (scale <= 0f) return null
            val bitmapWidth = (canvasWidth * scale).toInt().coerceAtLeast(1)
            val bitmapHeight = (contentHeight * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)

            canvas.save()
            canvas.translate(padding, padding)
            textLayout.draw(canvas)
            canvas.restore()

            // Tratti alle loro posizioni originali: la scala del canvas li
            // ridimensiona insieme al resto della pagina.
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            note.strokes.forEach { stroke ->
                // Stessa scelta dell'inchiostro di renderStrokesBitmap: il
                // fallback segue il tema del widget, non e' fisso.
                paint.color = (if (stroke.color != 0L) stroke.color else note.inkColor)
                    .takeIf { it != 0L }?.toInt()
                    ?: context.getColor(R.color.widget_text)
                paint.strokeWidth = stroke.widthDp * density
                val points = stroke.points
                if (points.size >= 4) {
                    val path = android.graphics.Path()
                    path.moveTo(points[0], points[1])
                    var i = 2
                    while (i + 1 < points.size) {
                        path.lineTo(points[i], points[i + 1])
                        i += 2
                    }
                    canvas.drawPath(path, paint)
                } else if (points.size == 2) {
                    canvas.drawPoint(points[0], points[1], paint)
                }
            }
            return bitmap
        }

        /**
         * Testo della nota con la formattazione tradotta in span del framework.
         * [spPx] e' il fattore sp in px (densita' x fontScale), come nell'editor.
         */
        private fun styledText(note: NoteData, spPx: Float): SpannableString {
            val spannable = SpannableString(note.text)
            note.formats.forEach { span ->
                val start = span.start.coerceIn(0, spannable.length)
                val end = span.end.coerceIn(0, spannable.length)
                if (start >= end) return@forEach
                val what: Any? = when (span.kind) {
                    // Misura in px: il valore e' in sp come nell'editor.
                    FormatKind.SIZE -> AbsoluteSizeSpan((span.value * spPx).toInt(), false)
                    FormatKind.COLOR -> ForegroundColorSpan(span.value.toInt())
                    FormatKind.BOLD -> StyleSpan(if (span.value == 1L) Typeface.BOLD else Typeface.NORMAL)
                    FormatKind.ITALIC -> StyleSpan(if (span.value == 1L) Typeface.ITALIC else Typeface.NORMAL)
                    FormatKind.UNDERLINE -> if (span.value == 1L) UnderlineSpan() else null
                    FormatKind.STRIKE -> if (span.value == 1L) StrikethroughSpan() else null
                }
                if (what != null) {
                    spannable.setSpan(what, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            return spannable
        }
    }
}
