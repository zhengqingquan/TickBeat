package com.tickbeat.app.domain

enum class NoteClef(val labelZh: String, val vexName: String) {
    Treble("高音谱号", "treble"),
    Bass("低音谱号", "bass");
}

/** One-octave practice ranges (inclusive high C). */
enum class NoteRange(
    val lowMidi: Int,
    val highMidi: Int,
    val labelZh: String
) {
    C2C3(36, 48, "C2–C3"),
    C3C4(48, 60, "C3–C4"),
    C4C5(60, 72, "C4–C5"),
    C5C6(72, 84, "C5–C6");

    fun suitableFor(clef: NoteClef): Boolean = when (clef) {
        // Avoid extreme ledger lines on each clef.
        NoteClef.Treble -> this != C2C3
        NoteClef.Bass -> this != C5C6
    }

    companion object {
        fun forClef(clef: NoteClef): List<NoteRange> =
            entries.filter { it.suitableFor(clef) }

        fun defaultFor(clef: NoteClef): NoteRange = when (clef) {
            NoteClef.Treble -> C4C5
            NoteClef.Bass -> C3C4
        }
    }
}

data class NoteReadQuestion(val midi: Int)

object NoteReadFactory {
    fun candidates(range: NoteRange, allowAccidentals: Boolean): List<NoteReadQuestion> {
        return buildList {
            for (midi in range.lowMidi..range.highMidi) {
                if (!allowAccidentals && IntervalRange.hasAccidental(midi)) continue
                add(NoteReadQuestion(midi))
            }
        }
    }

    fun random(
        seed: Int,
        range: NoteRange = NoteRange.C4C5,
        allowAccidentals: Boolean = true
    ): NoteReadQuestion {
        val rnd = kotlin.random.Random(seed)
        var pool = candidates(range, allowAccidentals)
        if (pool.isEmpty() && !allowAccidentals) {
            pool = candidates(range, allowAccidentals = true)
        }
        require(pool.isNotEmpty()) { "No notes for $range accidentals=$allowAccidentals" }
        return pool[rnd.nextInt(pool.size)]
    }

    /** VexFlow key string, e.g. "c/4", "c#/5". */
    fun vexKey(midi: Int): String {
        val names = arrayOf("c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b")
        val pc = ((midi % 12) + 12) % 12
        val octave = midi / 12 - 1
        return "${names[pc]}/$octave"
    }
}
