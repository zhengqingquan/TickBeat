package com.tickbeat.app.metronome

data class TimeSignature(
    val beats: Int,
    val noteValue: Int
) {
    val label: String get() = "$beats/$noteValue"

    companion object {
        val Default = TimeSignature(4, 4)

        /** Upper number (beats per bar). */
        val BeatOptions: List<Int> = (1..12).toList()

        /** Lower number (note value that gets one beat). */
        val NoteValueOptions: List<Int> = listOf(1, 2, 4, 8, 16)

        /** Common simple and compound meters for one-tap selection. */
        val Presets: List<TimeSignature> = listOf(
            TimeSignature(2, 4),
            TimeSignature(3, 4),
            TimeSignature(4, 4),
            TimeSignature(5, 4),
            TimeSignature(6, 4),
            TimeSignature(7, 4),
            TimeSignature(3, 8),
            TimeSignature(5, 8),
            TimeSignature(6, 8),
            TimeSignature(7, 8),
            TimeSignature(9, 8),
            TimeSignature(12, 8)
        )

        fun sanitize(beats: Int, noteValue: Int): TimeSignature {
            val safeBeats = beats.coerceIn(BeatOptions.first(), BeatOptions.last())
            val safeNote = if (noteValue in NoteValueOptions) {
                noteValue
            } else {
                NoteValueOptions.minBy { kotlin.math.abs(it - noteValue) }
            }
            return TimeSignature(safeBeats, safeNote)
        }
    }
}
