package com.tickbeat.app.domain

enum class CorruptionType {
    ShiftEarly,
    ShiftLate,
    DropHit,
    ExtraHit
}

data class EarQuestion(
    val id: String,
    val prompt: String,
    val base: Beatmap,
    val corrupt: Beatmap,
    val corruptionType: CorruptionType,
    val wrongEventIndex: Int,
    val choices: List<String>,
    val correctChoiceIndex: Int
)

object EarQuestionFactory {
    fun fromBeatmap(base: Beatmap, seed: Int = base.id.hashCode()): EarQuestion {
        val rnd = kotlin.random.Random(seed)
        val type = CorruptionType.entries[rnd.nextInt(CorruptionType.entries.size)]
        val shiftBeats = 0.25

        return when (type) {
            CorruptionType.ShiftEarly, CorruptionType.ShiftLate -> {
                val idx = if (base.events.size <= 1) 0 else 1 + rnd.nextInt(base.events.size - 1)
                val delta = if (type == CorruptionType.ShiftEarly) -shiftBeats else shiftBeats
                val newEvents = base.events.mapIndexed { i, e ->
                    if (i == idx) e.copy(tBeat = (e.tBeat + delta).coerceAtLeast(0.0)) else e
                }.sortedBy { it.tBeat }
                val corrupt = base.copy(id = "${base.id}_corrupt_$type", events = newEvents)
                val choices = listOf(
                    "第 ${idx + 1} 拍偏早",
                    "第 ${idx + 1} 拍偏晚",
                    "少打了一拍",
                    "多打了一拍"
                )
                EarQuestion(
                    id = "${base.id}_q_$type",
                    prompt = "听这段节奏，哪里不对？",
                    base = base,
                    corrupt = corrupt,
                    corruptionType = type,
                    wrongEventIndex = idx,
                    choices = choices,
                    correctChoiceIndex = if (type == CorruptionType.ShiftEarly) 0 else 1
                )
            }
            CorruptionType.DropHit -> {
                val idx = if (base.events.size <= 1) 0 else 1 + rnd.nextInt(base.events.size - 1)
                val newEvents = base.events.filterIndexed { i, _ -> i != idx }
                EarQuestion(
                    id = "${base.id}_q_drop",
                    prompt = "听这段节奏，哪里不对？",
                    base = base,
                    corrupt = base.copy(id = "${base.id}_drop", events = newEvents),
                    corruptionType = type,
                    wrongEventIndex = idx,
                    choices = listOf("少打了一拍", "多打了一拍", "整体偏快", "整体偏慢"),
                    correctChoiceIndex = 0
                )
            }
            CorruptionType.ExtraHit -> {
                val insertAt = rnd.nextDouble() * base.durationBeats.coerceAtLeast(1.0)
                val newEvents = (base.events + BeatEvent(tBeat = insertAt)).sortedBy { it.tBeat }
                EarQuestion(
                    id = "${base.id}_q_extra",
                    prompt = "听这段节奏，哪里不对？",
                    base = base,
                    corrupt = base.copy(id = "${base.id}_extra", events = newEvents),
                    corruptionType = type,
                    wrongEventIndex = -1,
                    choices = listOf("少打了一拍", "多打了一拍", "整体偏快", "整体偏慢"),
                    correctChoiceIndex = 1
                )
            }
        }
    }
}
