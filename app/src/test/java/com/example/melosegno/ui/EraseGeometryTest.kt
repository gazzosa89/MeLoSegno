package com.example.melosegno.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EraseGeometryTest {

    /** Tratto orizzontale come quello disegnato dall'emulatore: y costante. */
    private fun horizontalStroke(): List<Float> = buildList {
        for (x in 100..380 step 10) addAll(listOf(x.toFloat(), 132f))
    }

    private fun List<List<Float>>.allPoints(): Set<Pair<Float, Float>> =
        flatMap { it.chunked(2) }.map { it[0] to it[1] }.toSet()

    @Test
    fun tapOnStrokeLeavesAGap() {
        val stroke = horizontalStroke()
        val pieces = erasePoints(
            stroke,
            Offset(240f, 132f), // tap esattamente sul tratto
            Offset(240f, 132f),
            radius = 10.5f
        )
        // Il tratto si spezza in due pezzi con un buco nel mezzo.
        assertTrue("aspettavo 2 pezzi, trovati ${pieces.size}", pieces.size == 2)
        val kept = pieces.allPoints()
        assertTrue("il punto centrale non deve sopravvivere", !kept.contains(240f to 132f))
        assertTrue("i bordi del tratto devono restare", kept.contains(100f to 132f) && kept.contains(380f to 132f))
    }

    @Test
    fun fastSwipeThroughStrokeLeavesAGap() {
        val stroke = horizontalStroke()
        val pieces = erasePoints(
            stroke,
            Offset(130f, 150f),
            Offset(420f, 120f),
            radius = 10.5f
        )
        val kept = pieces.allPoints()
        // Nessun punto sopravvive dentro l'area spazzata.
        assertTrue(
            "punti rimasti dentro l'area cancellata",
            kept.none { (x, y) ->
                val d = distance(Offset(x, y), Offset(130f, 150f), Offset(420f, 120f))
                d <= 10.5f
            }
        )
    }

    @Test
    fun tapAwayLeavesStrokeIntact() {
        val stroke = horizontalStroke()
        val pieces = erasePoints(
            stroke,
            Offset(240f, 500f), // lontano dal tratto
            Offset(240f, 500f),
            radius = 10.5f
        )
        assertEquals(1, pieces.size)
        assertEquals(stroke.size, pieces[0].size)
    }

    @Test
    fun wholeStrokeInsideAreaDisappears() {
        val stroke = listOf(200f, 132f, 260f, 132f)
        val pieces = erasePoints(
            stroke,
            Offset(150f, 132f),
            Offset(300f, 132f),
            radius = 10.5f
        )
        assertTrue("il tratto interamente coperto deve sparire", pieces.isEmpty())
    }

    @Test
    fun longSegmentMiddleIsHit() {
        // Un solo segmento lungo 280px: il vecchio bug era che la gomma passata
        // a metà non cancellava nulla (nessun punto campionato lì).
        val stroke = listOf(100f, 132f, 380f, 132f)
        val pieces = erasePoints(
            stroke,
            Offset(240f, 132f),
            Offset(240f, 132f),
            radius = 10.5f
        )
        val kept = pieces.allPoints()
        assertTrue("il mezzo del segmento lungo deve essere cancellato", kept.none { it.second == 132f && it.first > 230f && it.first < 250f })
        assertTrue("devono restare i due pezzi esterni", pieces.size == 2)
    }
}
