package com.tickbeat.app.metronome

data class TimeSignature(
    val beats: Int,
    val noteValue: Int
) {
    val label: String get() = "$beats/$noteValue"

    companion object {
        val Default = TimeSignature(4, 4)

        /** Common simple and compound meters for practice. */
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
    }
}
