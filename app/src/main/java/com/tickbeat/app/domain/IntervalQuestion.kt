package com.tickbeat.app.domain

/** User-facing filter buckets for interval drill. */
enum class IntervalQuality(val labelZh: String) {
    Perfect("纯"),
    MajorMinor("大/小"),
    AugDim("增/减"),
    DoubleAugDim("倍增/倍减");
}

/**
 * Intervals practiced within one octave (keyboard answer = root + semitones).
 * 增减音程与大小/纯音程可能同半音距（等音），题干名称不同，用于辨名练习。
 */
enum class MusicalInterval(
    val semitones: Int,
    val labelZh: String,
    val quality: IntervalQuality
) {
    // 自然音程：小/大/纯
    Minor2(1, "小二度", IntervalQuality.MajorMinor),
    Major2(2, "大二度", IntervalQuality.MajorMinor),
    Minor3(3, "小三度", IntervalQuality.MajorMinor),
    Major3(4, "大三度", IntervalQuality.MajorMinor),
    Perfect4(5, "纯四度", IntervalQuality.Perfect),
    Perfect5(7, "纯五度", IntervalQuality.Perfect),
    Minor6(8, "小六度", IntervalQuality.MajorMinor),
    Major6(9, "大六度", IntervalQuality.MajorMinor),
    Minor7(10, "小七度", IntervalQuality.MajorMinor),
    Major7(11, "大七度", IntervalQuality.MajorMinor),
    Perfect8(12, "纯八度", IntervalQuality.Perfect),

    // 增减音程（含与上面等音者，如增四=减五=6）
    Aug1(1, "增一度", IntervalQuality.AugDim),
    Aug2(3, "增二度", IntervalQuality.AugDim),
    Dim3(2, "减三度", IntervalQuality.AugDim),
    Aug3(5, "增三度", IntervalQuality.AugDim),
    Dim4(4, "减四度", IntervalQuality.AugDim),
    Aug4(6, "增四度", IntervalQuality.AugDim),
    Dim5(6, "减五度", IntervalQuality.AugDim),
    Aug5(8, "增五度", IntervalQuality.AugDim),
    Dim6(7, "减六度", IntervalQuality.AugDim),
    Aug6(10, "增六度", IntervalQuality.AugDim),
    Dim7(9, "减七度", IntervalQuality.AugDim),
    Aug7(11, "增七度", IntervalQuality.AugDim),
    Dim8(11, "减八度", IntervalQuality.AugDim),

    // 倍增 / 倍减（半音距 1–12；倍增七/八超出单八度，不收录）
    DAug1(2, "倍增一度", IntervalQuality.DoubleAugDim),
    DAug2(4, "倍增二度", IntervalQuality.DoubleAugDim),
    DDim3(1, "倍减三度", IntervalQuality.DoubleAugDim),
    DAug3(6, "倍增三度", IntervalQuality.DoubleAugDim),
    DDim4(3, "倍减四度", IntervalQuality.DoubleAugDim),
    DAug4(7, "倍增四度", IntervalQuality.DoubleAugDim),
    DDim5(5, "倍减五度", IntervalQuality.DoubleAugDim),
    DAug5(9, "倍增五度", IntervalQuality.DoubleAugDim),
    DDim6(6, "倍减六度", IntervalQuality.DoubleAugDim),
    DAug6(11, "倍增六度", IntervalQuality.DoubleAugDim),
    DDim7(8, "倍减七度", IntervalQuality.DoubleAugDim),
    DDim8(10, "倍减八度", IntervalQuality.DoubleAugDim);
}

data class IntervalQuestion(
    val rootMidi: Int,
    val interval: MusicalInterval
) {
    val answerMidi: Int get() = rootMidi + interval.semitones
    val prompt: String get() = "弹出：${interval.labelZh}"
}

object IntervalRange {
    const val LOW_MIDI = 60  // C4
    const val HIGH_MIDI = 72 // C5

    /** Pitch classes with sharps on the keyboard (black keys). */
    private val accidentalPcs = setOf(1, 3, 6, 8, 10)

    fun hasAccidental(midi: Int): Boolean =
        ((midi % 12) + 12) % 12 in accidentalPcs
}

object IntervalQuestionFactory {
    private val noteNames = arrayOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    )

    fun midiName(midi: Int): String {
        val name = noteNames[((midi % 12) + 12) % 12]
        val octave = midi / 12 - 1
        return "$name$octave"
    }

    /**
     * @param allowAccidentals when false, both root and answer must be white keys.
     */
    fun candidates(
        qualities: Set<IntervalQuality>,
        allowAccidentals: Boolean = true
    ): List<IntervalQuestion> {
        val allowed = qualities.ifEmpty { IntervalQuality.entries.toSet() }
        return buildList {
            for (root in IntervalRange.LOW_MIDI until IntervalRange.HIGH_MIDI) {
                for (interval in MusicalInterval.entries) {
                    if (interval.quality !in allowed) continue
                    val answer = root + interval.semitones
                    if (answer !in IntervalRange.LOW_MIDI..IntervalRange.HIGH_MIDI) continue
                    if (!allowAccidentals &&
                        (IntervalRange.hasAccidental(root) || IntervalRange.hasAccidental(answer))
                    ) {
                        continue
                    }
                    add(IntervalQuestion(root, interval))
                }
            }
        }
    }

    fun random(
        seed: Int,
        qualities: Set<IntervalQuality> = IntervalQuality.entries.toSet(),
        allowAccidentals: Boolean = true
    ): IntervalQuestion {
        val rnd = kotlin.random.Random(seed)
        var pool = candidates(qualities, allowAccidentals)
        // Rare combo with white keys only may be empty — fall back to allowing #.
        if (pool.isEmpty() && !allowAccidentals) {
            pool = candidates(qualities, allowAccidentals = true)
        }
        require(pool.isNotEmpty()) {
            "No legal interval questions for $qualities accidentals=$allowAccidentals in C4–C5"
        }
        return pool[rnd.nextInt(pool.size)]
    }
}
