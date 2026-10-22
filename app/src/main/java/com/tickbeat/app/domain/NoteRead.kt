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
    C1C2(24, 36, "C1–C2"),
    C2C3(36, 48, "C2–C3"),
    C3C4(48, 60, "C3–C4"),
    C4C5(60, 72, "C4–C5"),
    C5C6(72, 84, "C5–C6"),
    C6C7(84, 96, "C6–C7");

    fun suitableFor(clef: NoteClef): Boolean = when (clef) {
        // Avoid extreme ledger lines on each clef.
        NoteClef.Treble -> this != C1C2 && this != C2C3
        NoteClef.Bass -> this != C5C6 && this != C6C7
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

    /** Pick a random clef + suitable octave, then a note in that octave. */
    fun randomAny(
        seed: Int,
        allowAccidentals: Boolean = true
    ): Triple<NoteClef, NoteRange, NoteReadQuestion> {
        val rnd = kotlin.random.Random(seed)
        val clef = NoteClef.entries[rnd.nextInt(NoteClef.entries.size)]
        val ranges = NoteRange.forClef(clef)
        val range = ranges[rnd.nextInt(ranges.size)]
        val q = random(seed = seed * 17 + 3, range = range, allowAccidentals = allowAccidentals)
        return Triple(clef, range, q)
    }

    /** VexFlow key string, e.g. "c/4", "c#/5". */
    fun vexKey(midi: Int): String {
        val names = arrayOf("c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b")
        val pc = ((midi % 12) + 12) % 12
        val octave = midi / 12 - 1
        return "${names[pc]}/$octave"
    }
}
