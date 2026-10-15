package com.tickbeat.app.domain

import kotlin.math.abs

enum class HitGrade {
    Perfect,
    Good,
    Miss
}

data class HitResult(
    val expectedMs: Long,
    val actualMs: Long?,
    val errorMs: Long?,
    val grade: HitGrade
)

data class SessionScore(
    val results: List<HitResult>,
    val perfectCount: Int,
    val goodCount: Int,
    val missCount: Int,
    val averageAbsErrorMs: Double,
    val maxCombo: Int,
    val score: Int
) {
    val total: Int get() = results.size
    val accuracyPercent: Int
        get() = if (total == 0) {
            0
        } else {
            (perfectCount * 100 + goodCount * 60) / total
        }
}

object JudgeConfig {
    const val PerfectWindowMs = 40L
    const val GoodWindowMs = 80L
}

object Judge {
    /**
     * Greedy nearest-tap matching within [JudgeConfig.GoodWindowMs].
     * Extra taps are ignored; unmatched expectations become Miss.
     */
    fun evaluate(
        expectedMs: List<Long>,
        tapMs: List<Long>,
        perfectWindowMs: Long = JudgeConfig.PerfectWindowMs,
        goodWindowMs: Long = JudgeConfig.GoodWindowMs
    ): SessionScore {
        val usedTaps = BooleanArray(tapMs.size)
        val results = ArrayList<HitResult>(expectedMs.size)
        var combo = 0
        var maxCombo = 0
        var errorSum = 0.0
        var errorCount = 0
        var perfect = 0
        var good = 0
        var miss = 0
        var score = 0

        for (expected in expectedMs) {
            var bestIdx = -1
            var bestDist = Long.MAX_VALUE
            for (i in tapMs.indices) {
                if (usedTaps[i]) continue
                val dist = abs(tapMs[i] - expected)
                if (dist < bestDist) {
                    bestDist = dist
                    bestIdx = i
                }
            }

            val hit = if (bestIdx >= 0 && bestDist <= goodWindowMs) {
                usedTaps[bestIdx] = true
                val actual = tapMs[bestIdx]
                val err = actual - expected
                val grade = if (bestDist <= perfectWindowMs) HitGrade.Perfect else HitGrade.Good
                errorSum += abs(err).toDouble()
                errorCount++
                when (grade) {
                    HitGrade.Perfect -> {
                        perfect++
                        combo++
                        score += 100
                    }
                    HitGrade.Good -> {
                        good++
                        combo++
                        score += 60
                    }
                    HitGrade.Miss -> Unit
                }
                maxCombo = maxOf(maxCombo, combo)
                HitResult(expected, actual, err, grade)
            } else {
                miss++
                combo = 0
                HitResult(expected, null, null, HitGrade.Miss)
            }
            results.add(hit)
        }

        val avg = if (errorCount == 0) 0.0 else errorSum / errorCount
        return SessionScore(
            results = results,
            perfectCount = perfect,
            goodCount = good,
            missCount = miss,
            averageAbsErrorMs = avg,
            maxCombo = maxCombo,
            score = score
        )
    }
}
