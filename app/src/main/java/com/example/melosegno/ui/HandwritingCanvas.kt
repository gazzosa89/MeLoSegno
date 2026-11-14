package com.example.melosegno.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.melosegno.data.Stroke as DataStroke

/** Tratto pronto per il disegno: punti, spessore e colore (0L = colore del tema). */
data class InkStroke(val points: List<Offset>, val width: Dp, val color: Long)

/**
 * Livello dell'inchiostro: disegna i tratti e, se [interactive], li gestisce.
 *
 * Ogni tratto mantiene il proprio spessore e colore, scelti al momento del
 * disegno: cambiare gli strumenti non altera i tratti gia' presenti.
 * Con la gomma attiva il trascinamento rimuove solo i punti toccati.
 * I callback passano sempre per [rememberUpdatedState]: il gesto usa cosi'
 * gli ultimi valori, non quelli catturati alla creazione del detector.
 */
@Composable
fun HandwritingCanvas(
    strokes: List<InkStroke>,
    interactive: Boolean,
    eraserEnabled: Boolean,
    defaultColor: Color,
    currentColor: Long,
    currentWidth: Dp,
    onStrokeFinished: (InkStroke) -> Unit,
    onErase: (from: Offset, to: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnStrokeFinished by rememberUpdatedState(onStrokeFinished)
    val currentOnErase by rememberUpdatedState(onErase)
    val currentInkWidth by rememberUpdatedState(currentWidth)
    val currentInkColor by rememberUpdatedState(currentColor)
    // Tratto in corso, disegnato subito per non far percepire ritardi.
    val currentStroke = remember { mutableStateOf<List<Offset>>(emptyList()) }

    val gestureModifier = if (interactive) {
        Modifier
            .pointerInput(eraserEnabled) {
                if (eraserEnabled) {
                    // Un semplice tocco, senza trascinare, deve cancellare:
                    // il rilevatore di trascinamento si attiva solo dopo la
                    // soglia di movimento, quindi per il tap serve questo.
                    detectTapGestures { position -> currentOnErase(position, position) }
                }
            }
            .pointerInput(eraserEnabled) {
                detectDragGestures(
                    onDragStart = { position ->
                        if (!eraserEnabled) currentStroke.value = listOf(position)
                    },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val position = change.position
                    if (eraserEnabled) {
                        currentOnErase(position - dragAmount, position)
                    } else {
                        currentStroke.value = currentStroke.value + position
                    }
                },
                onDragEnd = {
                    if (!eraserEnabled && currentStroke.value.size > 1) {
                        currentOnStrokeFinished(
                            InkStroke(currentStroke.value, currentInkWidth, currentInkColor)
                        )
                    }
                    currentStroke.value = emptyList()
                },
                onDragCancel = { currentStroke.value = emptyList() }
                )
            }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.then(gestureModifier)) {
        val visibleStrokes = strokes + if (currentStroke.value.size > 1) {
            listOf(InkStroke(currentStroke.value, currentInkWidth, currentInkColor))
        } else {
            emptyList()
        }
        visibleStrokes.forEach { ink ->
            if (ink.points.size > 1) {
                drawPath(
                    path = ink.points.toPath(),
                    color = if (ink.color != 0L) Color(ink.color) else defaultColor,
                    style = Stroke(
                        width = ink.width.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

// ---------- Conversioni e geometria dei tratti ----------

/** Da coordinate piatte [x1, y1, x2, y2, ...] a punti. */
internal fun List<Float>.strokeToPoints(): List<Offset> =
    chunked(2).map { (x, y) -> Offset(x, y) }

/** Da punti a coordinate piatte, per la persistenza. */
internal fun List<Offset>.strokeToFlat(): List<Float> =
    flatMap { listOf(it.x, it.y) }

internal fun List<Offset>.toPath(): Path {
    val path = Path()
    if (isEmpty()) return path
    path.moveTo(first().x, first().y)
    drop(1).forEach { path.lineTo(it.x, it.y) }
    return path
}

/**
 * Gomma a campionamento: percorre la polilinea del tratto e scarta i punti che
 * cadono nell'area spazzata dalla gomma. I segmenti piu' lunghi del raggio
 * vengono campionati finemente (altrimenti una gomma passata veloce nel mezzo
 * non li intercetterebbe mai); i segmenti intatti conservano i punti originali.
 * Uscendo dall'area cancellata la spezzata si chiude e ne comincia una nuova:
 * altrimenti il `lineTo` di rendering ridisegnerebbe l'inchiostro sopra il buco.
 */
internal fun erasePoints(
    points: List<Float>,
    from: Offset,
    to: Offset,
    radius: Float
): List<List<Float>> {
    val pts = points.strokeToPoints()
    if (pts.isEmpty()) return emptyList()
    if (pts.size == 1) {
        // Tratto di un solo punto (un pallino): si cancella se toccato.
        return if (pointHitsSegment(pts[0], from, to, radius)) emptyList() else listOf(points)
    }

    // True se il punto p cade nell'area cancellata (capsula attorno a [from, to]).
    fun inside(p: Offset): Boolean = pointHitsSegment(p, from, to, radius)

    val pieces = mutableListOf<List<Offset>>()
    var current = mutableListOf<Offset>()
    var insidePrev = inside(pts[0])
    if (!insidePrev) current += pts[0]

    for (i in 1 until pts.size) {
        val a = pts[i - 1]
        val b = pts[i]
        val steps = maxOf(1, kotlin.math.ceil((b - a).getDistance() / (radius / 2f)).toInt())
        // Pre-analisi: il segmento viene toccato dalla gomma?
        var segmentHit = inside(a)
        val samples = ArrayList<Offset>(steps)
        for (s in 1..steps) {
            val p = a + (b - a) * (s.toFloat() / steps)
            samples += p
            if (inside(p)) segmentHit = true
        }
        if (!segmentHit) {
            // Segmento intatto: si conserva il punto originale, senza densificarlo.
            if (insidePrev) {
                if (current.size > 1) pieces += current
                current = mutableListOf()
                insidePrev = false
            }
            current += b
        } else {
            for (p in samples) {
                val isIn = inside(p)
                if (!isIn && insidePrev) {
                    // Uscita dall'area cancellata: nuova spezzata da qui.
                    current = mutableListOf(p)
                } else if (!isIn) {
                    current += p
                } else if (!insidePrev) {
                    // Entrata nell'area cancellata: si chiude la spezzata.
                    if (current.size > 1) pieces += current
                    current = mutableListOf()
                }
                insidePrev = isIn
            }
        }
    }
    if (current.size > 1) pieces += current
    return pieces.map { it.strokeToFlat() }
}

/** Applica la gomma a tutti i tratti, preservandone spessore e colore. */
internal fun eraseFromStrokes(
    strokes: List<DataStroke>,
    from: Offset,
    to: Offset,
    radius: Float,
    pxPerDp: Float
): List<DataStroke> = buildList {
    strokes.forEach { stroke ->
        // L'inchiostro sborda di mezzo spessore oltre la linea centrale:
        // aggiungerlo al raggio fa terminare il tratto esattamente al bordo
        // del cerchio della gomma, a qualunque spessore.
        val effectiveRadius = radius + stroke.widthDp * pxPerDp / 2f
        erasePoints(stroke.points, from, to, effectiveRadius).forEach { piece ->
            add(stroke.copy(points = piece))
        }
    }
}

/** True se il punto [p] cade nel raggio dal segmento [a, b]. */
internal fun pointHitsSegment(p: Offset, a: Offset, b: Offset, radius: Float): Boolean =
    distance(p, a, b) <= radius

/** Proiezione di [p] sul segmento [a, b]. */
private fun projectOnSegment(p: Offset, a: Offset, b: Offset): Offset {
    val ab = b - a
    val lengthSquared = ab.getDistanceSquared()
    if (lengthSquared == 0f) return a
    val t = (((p - a) dot ab) / lengthSquared).coerceIn(0f, 1f)
    return a + ab * t
}

private infix fun Offset.dot(other: Offset): Float = x * other.x + y * other.y

/** Distanza tra il punto [p] e il segmento [a, b]. */
internal fun distance(p: Offset, a: Offset, b: Offset): Float {
    val closest = projectOnSegment(p, a, b)
    return (p - closest).getDistance()
}

