package com.tickbeat.app.domain

/**
 * Rhythm content in beat units. Runtime ms = tBeat * 60_000 / bpm.
 */
data class BeatEvent(
    val tBeat: Double,
    val accent: Boolean = false
)

data class Beatmap(
    val id: String,
    val title: String,
    val bpm: Int,
    val beatsPerBar: Int,
    val noteValue: Int = 4,
    val difficulty: Int = 1,
    val tags: Set<String> = emptySet(),
    val events: List<BeatEvent>
) {
    val durationBeats: Double
        get() = events.maxOfOrNull { it.tBeat }?.plus(1.0) ?: 0.0

    fun expectedTimesMs(startEpochMs: Long = 0L): List<Long> {
        val beatMs = 60_000.0 / bpm.coerceAtLeast(1)
        return events.map { startEpochMs + (it.tBeat * beatMs).toLong() }
    }

    fun hasTag(tag: String): Boolean = tag in tags

    /**
     * Repeat this map across [bars] bars using [beatsPerBar] as the stride
     * (not [durationBeats]), so back-to-back bars have no extra gap.
     */
    fun repeatAcrossBars(bars: Int): Beatmap {
        val n = bars.coerceAtLeast(1)
        if (n == 1) return this
        val barLen = beatsPerBar.toDouble().coerceAtLeast(1.0)
        val expanded = buildList {
            for (pass in 0 until n) {
                val offset = pass * barLen
                for (e in events) add(e.copy(tBeat = e.tBeat + offset))
            }
        }
        return copy(id = "${id}_x$n", events = expanded)
    }
}

object BeatmapFactory {
    /** Steady quarter notes for [bars] bars — used by follow mode. */
    fun steadyQuarters(
        id: String,
        title: String,
        bpm: Int,
        beatsPerBar: Int,
        bars: Int,
        tags: Set<String> = setOf("follow")
    ): Beatmap {
        val events = buildList {
            val total = beatsPerBar * bars
            for (i in 0 until total) {
                add(BeatEvent(tBeat = i.toDouble(), accent = i % beatsPerBar == 0))
            }
        }
        return Beatmap(
            id = id,
            title = title,
            bpm = bpm,
            beatsPerBar = beatsPerBar,
            difficulty = 1,
            tags = tags,
            events = events
        )
    }
}
