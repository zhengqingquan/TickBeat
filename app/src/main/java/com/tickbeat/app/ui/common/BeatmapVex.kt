package com.tickbeat.app.ui.common

import com.tickbeat.app.domain.Beatmap
import kotlin.math.floor
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

/**
 * Beatmap → JSON for the offline VexFlow WebView renderer.
 *
 * Each item is either a note `{dur, accent, tBeat}` or a rest `{dur, rest:true, tBeat}`
 * where [dur] is a VexFlow duration: 16, 8, q, qd, h, 8r, qr, …
 */
fun Beatmap.toVexScoreJson(): String {
    val events = events.sortedBy { it.tBeat }
    val barLen = beatsPerBar.coerceAtLeast(1)
    val eighthsPerBar = barLen * 2
    val notes = JSONArray()
    var cursor = 0

    fun emitRest(eighths: Int, atBeat: Double) {
        var left = eighths
        var t = atBeat
        while (left > 0) {
            val n = when {
                left >= 4 -> 4
                left >= 2 -> 2
                else -> 1
            }
            notes.put(
                JSONObject()
                    .put("dur", eighthsToVexDur(n, rest = true))
                    .put("rest", true)
                    .put("tBeat", t)
            )
            t += n / 2.0
            left -= n
            cursor += n
        }
    }

    fun emitNote(eighths: Int, accent: Boolean, atBeat: Double) {
        notes.put(
            JSONObject()
                .put("dur", eighthsToVexDur(eighths, rest = false))
                .put("accent", accent)
                .put("rest", false)
                .put("tBeat", atBeat)
        )
        cursor += eighths
    }

    if (events.isEmpty()) {
        emitRest(eighthsPerBar, 0.0)
    } else {
        events.forEachIndexed { index, event ->
            val start = beatsToEighths(event.tBeat)
            if (start > cursor) {
                emitRest(start - cursor, cursor / 2.0)
            }
            cursor = maxOf(cursor, start)

            val barEnd =
                ((floor(event.tBeat / barLen + 1e-9) + 1).toInt()) * eighthsPerBar
            val next = events.getOrNull(index + 1)?.let { beatsToEighths(it.tBeat) } ?: barEnd
            val ioi = (next - start).coerceAtLeast(1)

            if (ioi >= 4 && index < events.lastIndex) {
                emitNote(2, event.accent, event.tBeat)
                emitRest(ioi - 2, event.tBeat + 1.0)
            } else {
                emitNote(ioi, event.accent, event.tBeat)
            }
        }
        val rem = eighthsPerBar - (cursor % eighthsPerBar)
        if (rem in 1 until eighthsPerBar) {
            emitRest(rem, cursor / 2.0)
        }
    }

    return JSONObject()
        .put("beatsPerBar", beatsPerBar)
        .put("noteValue", noteValue)
        .put("notes", notes)
        .toString()
}

private fun beatsToEighths(beats: Double): Int =
    (beats * 2.0).roundToInt().coerceAtLeast(0)

private fun eighthsToVexDur(eighths: Int, rest: Boolean): String {
    val n = eighths.coerceAtLeast(1)
    val base = when (n) {
        1 -> "8"
        2 -> "q"
        3 -> "qd"
        4 -> "h"
        else -> if (n >= 4) "h" else "8"
    }
    return if (rest) {
        when (base) {
            "8" -> "8r"
            "q", "qd" -> "qr"
            "h" -> "hr"
            else -> "qr"
        }
    } else base
}
