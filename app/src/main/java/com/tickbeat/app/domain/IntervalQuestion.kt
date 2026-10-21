package com.tickbeat.app.domain

/**
 * Intervals practiced within one octave (keyboard answer = root + semitones).
 * 增减音程与大小/纯音程可能同半音距（等音），题干名称不同，用于辨名练习。
 */
enum class MusicalInterval(val semitones: Int, val labelZh: String) {
    // 自然音程：小/大/纯
    Minor2(1, "小二度"),
    Major2(2, "大二度"),
    Minor3(3, "小三度"),
    Major3(4, "大三度"),
    Perfect4(5, "纯四度"),
    Perfect5(7, "纯五度"),
    Minor6(8, "小六度"),
    Major6(9, "大六度"),
    Minor7(10, "小七度"),
    Major7(11, "大七度"),
    Perfect8(12, "纯八度"),

    // 增减音程（含与上面等音者，如增四=减五=6）
    Aug1(1, "增一度"),
    Aug2(3, "增二度"),
    Dim3(2, "减三度"),
    Aug3(5, "增三度"),
    Dim4(4, "减四度"),
    Aug4(6, "增四度"),
    Dim5(6, "减五度"),
    Aug5(8, "增五度"),
    Dim6(7, "减六度"),
    Aug6(10, "增六度"),
    Dim7(9, "减七度"),
    Aug7(11, "增七度"),
    Dim8(11, "减八度"),

    // 倍增 / 倍减（半音距 1–12；倍增七/八超出单八度，不收录）
    DAug1(2, "倍增一度"),
    DAug2(4, "倍增二度"),
    DDim3(1, "倍减三度"),
    DAug3(6, "倍增三度"),
    DDim4(3, "倍减四度"),
    DAug4(7, "倍增四度"),
    DDim5(5, "倍减五度"),
    DAug5(9, "倍增五度"),
    DDim6(6, "倍减六度"),
    DAug6(11, "倍增六度"),
    DDim7(8, "倍减七度"),
    DDim8(10, "倍减八度");
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

    fun random(seed: Int): IntervalQuestion {
        val rnd = kotlin.random.Random(seed)
        val candidates = buildList {
            for (root in IntervalRange.LOW_MIDI until IntervalRange.HIGH_MIDI) {
                for (interval in MusicalInterval.entries) {
                    val answer = root + interval.semitones
                    if (answer in IntervalRange.LOW_MIDI..IntervalRange.HIGH_MIDI) {
                        add(IntervalQuestion(root, interval))
                    }
                }
            }
        }
        require(candidates.isNotEmpty()) { "No legal interval questions in C4–C5" }
        return candidates[rnd.nextInt(candidates.size)]
    }
}
