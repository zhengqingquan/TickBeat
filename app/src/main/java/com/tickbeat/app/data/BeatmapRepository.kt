package com.tickbeat.app.data

import android.content.Context
import com.tickbeat.app.domain.BeatEvent
import com.tickbeat.app.domain.Beatmap
import com.tickbeat.app.domain.BeatmapFactory
import org.json.JSONObject

class BeatmapRepository private constructor(private val context: Context) {

    private val catalog: List<Beatmap> by lazy { builtIn() + loadFromAssets() }

    fun all(): List<Beatmap> = catalog

    companion object {
        @Volatile
        private var instance: BeatmapRepository? = null

        fun get(context: Context): BeatmapRepository {
            return instance ?: synchronized(this) {
                instance ?: BeatmapRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun byId(id: String): Beatmap? = catalog.find { it.id == id }

    fun byTag(tag: String): List<Beatmap> = catalog.filter { it.hasTag(tag) }

    private fun loadFromAssets(): List<Beatmap> {
        val am = context.assets
        val names = runCatching { am.list("beatmaps")?.toList().orEmpty() }.getOrDefault(emptyList())
        return names.filter { it.endsWith(".json") }.mapNotNull { file ->
            runCatching {
                am.open("beatmaps/$file").bufferedReader().use { it.readText() }.let(::parseJson)
            }.getOrNull()
        }
    }

    private fun parseJson(raw: String): Beatmap {
        val o = JSONObject(raw)
        val eventsJson = o.getJSONArray("events")
        val events = buildList {
            for (i in 0 until eventsJson.length()) {
                val e = eventsJson.getJSONObject(i)
                add(
                    BeatEvent(
                        tBeat = e.getDouble("tBeat"),
                        accent = e.optBoolean("accent", false)
                    )
                )
            }
        }
        val tagsJson = o.optJSONArray("tags")
        val tags = buildSet {
            if (tagsJson != null) {
                for (i in 0 until tagsJson.length()) add(tagsJson.getString(i))
            }
        }
        return Beatmap(
            id = o.getString("id"),
            title = o.getString("title"),
            bpm = o.getInt("bpm"),
            beatsPerBar = o.getInt("beatsPerBar"),
            noteValue = o.optInt("noteValue", 4),
            difficulty = o.optInt("difficulty", 1),
            tags = tags,
            events = events
        )
    }

    private fun builtIn(): List<Beatmap> = listOf(
        BeatmapFactory.steadyQuarters(
            id = "follow_120_4_8bars",
            title = "跟拍 · 120 BPM · 8 小节",
            bpm = 120,
            beatsPerBar = 4,
            bars = 8,
            tags = setOf("follow")
        ),
        BeatmapFactory.steadyQuarters(
            id = "follow_100_4_4bars",
            title = "跟拍 · 100 BPM · 4 小节",
            bpm = 100,
            beatsPerBar = 4,
            bars = 4,
            tags = setOf("follow")
        ),
        BeatmapFactory.steadyQuarters(
            id = "follow_90_3_4bars",
            title = "跟拍 · 90 BPM · 3/4",
            bpm = 90,
            beatsPerBar = 3,
            bars = 4,
            tags = setOf("follow")
        ),
        // Imitate / sightread patterns
        pattern(
            id = "imitate_quarters",
            title = "模仿 · 全四分",
            bpm = 100,
            beatsPerBar = 4,
            difficulty = 1,
            tags = setOf("imitate", "sightread", "ear"),
            beats = listOf(0.0 to true, 1.0 to false, 2.0 to false, 3.0 to false)
        ),
        pattern(
            id = "imitate_eighths",
            title = "模仿 · 八分连续",
            bpm = 90,
            beatsPerBar = 4,
            difficulty = 2,
            tags = setOf("imitate", "sightread", "ear"),
            // One bar: 8 eighths, accents on beats 1 and 3 (t=0, 2)
            beats = (0..7).map { i -> (i * 0.5) to (i % 4 == 0) }
        ),
        pattern(
            id = "imitate_rest",
            title = "模仿 · 带休止",
            bpm = 100,
            beatsPerBar = 4,
            difficulty = 2,
            tags = setOf("imitate", "sightread", "ear"),
            // One bar: ♩ ♩ 𝄽 ♩  (attack on 0,1,3 — beat 2 is rest)
            beats = listOf(0.0 to true, 1.0 to false, 3.0 to false)
        ),
        pattern(
            id = "imitate_syncopa",
            title = "模仿 · 切分",
            bpm = 96,
            beatsPerBar = 4,
            difficulty = 3,
            tags = setOf("imitate", "sightread", "ear"),
            // One bar: ♩. syncopation 0 · 0.5 · 1.5 · 2 · 3
            beats = listOf(0.0 to true, 0.5 to false, 1.5 to false, 2.0 to false, 3.0 to false)
        ),
        pattern(
            id = "imitate_dotted",
            title = "模仿 · 附点",
            bpm = 88,
            beatsPerBar = 4,
            difficulty = 3,
            tags = setOf("imitate", "sightread", "ear"),
            // One bar: ♩. ♪ ♩ ♩
            beats = listOf(0.0 to true, 1.5 to false, 2.0 to false, 3.0 to false)
        ),
        pattern(
            id = "sight_one_bar_eighth",
            title = "视奏 · 八分连续",
            bpm = 86,
            beatsPerBar = 4,
            difficulty = 2,
            tags = setOf("sightread"),
            // One bar only (was two bars; imitate must stay single-bar)
            beats = (0..7).map { i -> (i * 0.5) to (i % 4 == 0) }
        ),
        pattern(
            id = "ear_simple",
            title = "听辨 · 简单四拍",
            bpm = 100,
            beatsPerBar = 4,
            difficulty = 1,
            tags = setOf("ear"),
            beats = listOf(0.0 to true, 1.0 to false, 2.0 to false, 3.0 to false)
        )
    )

    private fun pattern(
        id: String,
        title: String,
        bpm: Int,
        beatsPerBar: Int,
        difficulty: Int,
        tags: Set<String>,
        beats: List<Pair<Double, Boolean>>
    ): Beatmap = Beatmap(
        id = id,
        title = title,
        bpm = bpm,
        beatsPerBar = beatsPerBar,
        difficulty = difficulty,
        tags = tags,
        events = beats.map { BeatEvent(tBeat = it.first, accent = it.second) }
    )
}
