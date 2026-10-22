package com.tickbeat.app.domain

enum class KeyMode(val labelZh: String) {
    Major("自然大调"),
    Minor("自然小调")
}

enum class AccidentalSide {
    None,
    Sharp,
    Flat
}

/**
 * A named key whose staff key signature is rendered via [vexKey]
 * (always the major / relative-major name VexFlow expects).
 */
data class MusicKey(
    val id: String,
    val labelZh: String,
    val vexKey: String,
    val mode: KeyMode,
    val side: AccidentalSide,
    val accidentalCount: Int
)

data class KeySigQuestion(
    val answer: MusicKey,
    val options: List<MusicKey>
)

object KeySigCatalog {
    /** 15 majors + 15 relative minors. */
    val all: List<MusicKey> = listOf(
        // Sharps / naturals
        mk("C", "C大调", "C", KeyMode.Major, AccidentalSide.None, 0),
        mk("a", "a小调", "C", KeyMode.Minor, AccidentalSide.None, 0),
        mk("G", "G大调", "G", KeyMode.Major, AccidentalSide.Sharp, 1),
        mk("e", "e小调", "G", KeyMode.Minor, AccidentalSide.Sharp, 1),
        mk("D", "D大调", "D", KeyMode.Major, AccidentalSide.Sharp, 2),
        mk("b", "b小调", "D", KeyMode.Minor, AccidentalSide.Sharp, 2),
        mk("A", "A大调", "A", KeyMode.Major, AccidentalSide.Sharp, 3),
        mk("f#", "f♯小调", "A", KeyMode.Minor, AccidentalSide.Sharp, 3),
        mk("E", "E大调", "E", KeyMode.Major, AccidentalSide.Sharp, 4),
        mk("c#", "c♯小调", "E", KeyMode.Minor, AccidentalSide.Sharp, 4),
        mk("B", "B大调", "B", KeyMode.Major, AccidentalSide.Sharp, 5),
        mk("g#", "g♯小调", "B", KeyMode.Minor, AccidentalSide.Sharp, 5),
        mk("F#", "F♯大调", "F#", KeyMode.Major, AccidentalSide.Sharp, 6),
        mk("d#", "d♯小调", "F#", KeyMode.Minor, AccidentalSide.Sharp, 6),
        mk("C#", "C♯大调", "C#", KeyMode.Major, AccidentalSide.Sharp, 7),
        mk("a#", "a♯小调", "C#", KeyMode.Minor, AccidentalSide.Sharp, 7),
        // Flats
        mk("F", "F大调", "F", KeyMode.Major, AccidentalSide.Flat, 1),
        mk("d", "d小调", "F", KeyMode.Minor, AccidentalSide.Flat, 1),
        mk("Bb", "B♭大调", "Bb", KeyMode.Major, AccidentalSide.Flat, 2),
        mk("g", "g小调", "Bb", KeyMode.Minor, AccidentalSide.Flat, 2),
        mk("Eb", "E♭大调", "Eb", KeyMode.Major, AccidentalSide.Flat, 3),
        mk("c", "c小调", "Eb", KeyMode.Minor, AccidentalSide.Flat, 3),
        mk("Ab", "A♭大调", "Ab", KeyMode.Major, AccidentalSide.Flat, 4),
        mk("f", "f小调", "Ab", KeyMode.Minor, AccidentalSide.Flat, 4),
        mk("Db", "D♭大调", "Db", KeyMode.Major, AccidentalSide.Flat, 5),
        mk("bb", "b♭小调", "Db", KeyMode.Minor, AccidentalSide.Flat, 5),
        mk("Gb", "G♭大调", "Gb", KeyMode.Major, AccidentalSide.Flat, 6),
        mk("eb", "e♭小调", "Gb", KeyMode.Minor, AccidentalSide.Flat, 6),
        mk("Cb", "C♭大调", "Cb", KeyMode.Major, AccidentalSide.Flat, 7),
        mk("ab", "a♭小调", "Cb", KeyMode.Minor, AccidentalSide.Flat, 7)
    )

    private fun mk(
        id: String,
        labelZh: String,
        vexKey: String,
        mode: KeyMode,
        side: AccidentalSide,
        count: Int
    ) = MusicKey(id, labelZh, vexKey, mode, side, count)

    fun forModes(modes: Set<KeyMode>): List<MusicKey> {
        if (modes.isEmpty()) return emptyList()
        return all.filter { it.mode in modes }
    }
}

object KeySigFactory {
    fun random(seed: Int, modes: Set<KeyMode>): KeySigQuestion {
        val rnd = kotlin.random.Random(seed)
        var pool = KeySigCatalog.forModes(modes)
        if (pool.isEmpty()) {
            pool = KeySigCatalog.all
        }
        val answer = pool[rnd.nextInt(pool.size)]
        val options = buildOptions(answer, pool, rnd)
        return KeySigQuestion(answer = answer, options = options)
    }

    /**
     * 4 choices including [answer]. Prefer same accidental side and nearby count;
     * fall back to the rest of the pool.
     */
    private fun buildOptions(
        answer: MusicKey,
        pool: List<MusicKey>,
        rnd: kotlin.random.Random
    ): List<MusicKey> {
        val others = pool.filter { it.id != answer.id }
        val preferred = others
            .filter { it.side == answer.side || answer.side == AccidentalSide.None }
            .sortedBy { kotlin.math.abs(it.accidentalCount - answer.accidentalCount) }
        val picks = mutableListOf<MusicKey>()
        for (k in preferred) {
            if (picks.size >= 3) break
            picks.add(k)
        }
        if (picks.size < 3) {
            val rest = others.filter { it !in picks }.shuffled(rnd)
            for (k in rest) {
                if (picks.size >= 3) break
                picks.add(k)
            }
        }
        // If pool is tiny, pad from full catalog (excluding answer).
        if (picks.size < 3) {
            val rest = KeySigCatalog.all.filter { it.id != answer.id && it !in picks }.shuffled(rnd)
            for (k in rest) {
                if (picks.size >= 3) break
                picks.add(k)
            }
        }
        return (picks.take(3) + answer).shuffled(rnd)
    }
}
