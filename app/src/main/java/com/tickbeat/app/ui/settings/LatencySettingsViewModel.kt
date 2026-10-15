package com.tickbeat.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tickbeat.app.audio.RhythmPlaybackEngine
import com.tickbeat.app.data.TimingPreferences
import com.tickbeat.app.domain.BeatmapFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

enum class LatencyPhase {
    Idle,
    Calibrating,
    Result
}

data class LatencyUiState(
    val offsetMs: Int = 0,
    val phase: LatencyPhase = LatencyPhase.Idle,
    val measuredMs: Int? = null,
    val matchedTaps: Int = 0,
    val beatIndex: Int = -1
)

class LatencySettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val timing = TimingPreferences.get(app)
    private val engine = RhythmPlaybackEngine()

    private val _ui = MutableStateFlow(LatencyUiState(offsetMs = timing.inputOffsetMs))
    val ui: StateFlow<LatencyUiState> = _ui.asStateFlow()

    private val taps = mutableListOf<Long>()
    private var expectedAudible = listOf<Long>()

    private val calibrateMap = BeatmapFactory.steadyQuarters(
        id = "latency_calibrate_100_8",
        title = "延迟校准",
        bpm = 100,
        beatsPerBar = 4,
        bars = 2,
        tags = emptySet()
    )

    init {
        engine.onEventAudible = { index, _ ->
            _ui.update { it.copy(beatIndex = index) }
        }
        engine.onStarted = { origin ->
            expectedAudible = calibrateMap.expectedTimesMs(origin)
        }
        engine.onCompleted = {
            finishCalibration()
        }
    }

    fun setOffset(ms: Int) {
        val clamped = ms.coerceIn(TimingPreferences.MIN_OFFSET, TimingPreferences.MAX_OFFSET)
        timing.inputOffsetMs = clamped
        _ui.update { it.copy(offsetMs = clamped) }
    }

    fun resetOffset() {
        setOffset(0)
        _ui.update { it.copy(measuredMs = null, matchedTaps = 0, phase = LatencyPhase.Idle) }
    }

    fun startCalibration() {
        taps.clear()
        expectedAudible = emptyList()
        _ui.update {
            it.copy(
                phase = LatencyPhase.Calibrating,
                measuredMs = null,
                matchedTaps = 0,
                beatIndex = -1
            )
        }
        engine.play(calibrateMap, muted = false)
    }

    fun onTap(eventTimeMs: Long) {
        if (_ui.value.phase != LatencyPhase.Calibrating) return
        taps.add(eventTimeMs)
    }

    fun stopCalibration() {
        engine.stop()
        if (_ui.value.phase == LatencyPhase.Calibrating) finishCalibration()
    }

    fun applyMeasured() {
        val measured = _ui.value.measuredMs ?: return
        setOffset(measured)
        _ui.update { it.copy(phase = LatencyPhase.Idle) }
    }

    fun discardResult() {
        _ui.update { it.copy(phase = LatencyPhase.Idle, measuredMs = null, matchedTaps = 0) }
    }

    fun stop() {
        engine.stop()
        if (_ui.value.phase == LatencyPhase.Calibrating) {
            _ui.update { it.copy(phase = LatencyPhase.Idle, beatIndex = -1) }
        }
    }

    private fun finishCalibration() {
        if (_ui.value.phase != LatencyPhase.Calibrating) return
        val expected = expectedAudible.ifEmpty {
            calibrateMap.expectedTimesMs(engine.audibleOriginUptimeMs)
        }
        val errors = matchErrors(expected, taps)
        val median = medianOrNull(errors)
        _ui.update {
            it.copy(
                phase = LatencyPhase.Result,
                measuredMs = median?.toInt()?.coerceIn(
                    TimingPreferences.MIN_OFFSET,
                    TimingPreferences.MAX_OFFSET
                ),
                matchedTaps = errors.size,
                beatIndex = -1
            )
        }
    }

    /**
     * Nearest-tap error (tap - expected) within the offset range; positive = late tap.
     */
    private fun matchErrors(expected: List<Long>, tapMs: List<Long>): List<Long> {
        val used = BooleanArray(tapMs.size)
        val errors = ArrayList<Long>(expected.size)
        for (exp in expected) {
            var bestIdx = -1
            var bestDist = Long.MAX_VALUE
            for (i in tapMs.indices) {
                if (used[i]) continue
                val dist = abs(tapMs[i] - exp)
                if (dist < bestDist) {
                    bestDist = dist
                    bestIdx = i
                }
            }
            if (bestIdx >= 0 && bestDist <= MATCH_WINDOW_MS) {
                used[bestIdx] = true
                errors.add(tapMs[bestIdx] - exp)
            }
        }
        return errors
    }

    private fun medianOrNull(values: List<Long>): Long? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2
        } else {
            sorted[mid]
        }
    }

    override fun onCleared() {
        engine.stop()
        engine.onEventAudible = null
        engine.onStarted = null
        engine.onCompleted = null
        super.onCleared()
    }

    companion object {
        private const val MATCH_WINDOW_MS = TimingPreferences.MAX_OFFSET.toLong()
    }
}
