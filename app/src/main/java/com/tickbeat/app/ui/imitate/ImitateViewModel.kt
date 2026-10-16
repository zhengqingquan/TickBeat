package com.tickbeat.app.ui.imitate

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tickbeat.app.audio.RhythmPlaybackEngine
import com.tickbeat.app.data.BeatmapRepository
import com.tickbeat.app.data.TimingPreferences
import com.tickbeat.app.domain.Beatmap
import com.tickbeat.app.domain.Judge
import com.tickbeat.app.domain.SessionScore
import com.tickbeat.app.domain.applyInputOffset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ImitatePhase {
    Pick,
    Demo,
    Record,
    Result
}

data class ImitateUiState(
    val maps: List<Beatmap> = emptyList(),
    val selected: Beatmap? = null,
    val phase: ImitatePhase = ImitatePhase.Pick,
    val demoPass: Int = 0,
    val score: SessionScore? = null,
    val playheadBeat: Double? = null
)

class ImitateViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BeatmapRepository.get(app)
    private val timing = TimingPreferences.get(app)
    private val engine = RhythmPlaybackEngine()

    private val _ui = MutableStateFlow(ImitateUiState())
    val ui: StateFlow<ImitateUiState> = _ui.asStateFlow()

    private val taps = mutableListOf<Long>()
    private var demoCountTarget = 2
    private var playheadJob: Job? = null

    init {
        val maps = repo.byTag("imitate")
        _ui.update { it.copy(maps = maps, selected = maps.firstOrNull()) }
        engine.onCompleted = {
            when (_ui.value.phase) {
                ImitatePhase.Demo -> {
                    val next = _ui.value.demoPass + 1
                    if (next < demoCountTarget) {
                        _ui.update { it.copy(demoPass = next) }
                        _ui.value.selected?.let { map ->
                            engine.play(map)
                            startPlayhead(map)
                        }
                    } else {
                        beginRecord()
                    }
                }
                ImitatePhase.Record -> finishRecord()
                else -> Unit
            }
        }
    }

    fun select(map: Beatmap) {
        if (_ui.value.phase == ImitatePhase.Demo || _ui.value.phase == ImitatePhase.Record) return
        _ui.update { it.copy(selected = map, score = null, phase = ImitatePhase.Pick) }
    }

    fun startDemo() {
        val map = _ui.value.selected ?: return
        taps.clear()
        playheadJob?.cancel()
        _ui.update {
            it.copy(phase = ImitatePhase.Demo, demoPass = 0, score = null, playheadBeat = 0.0)
        }
        engine.play(map)
        startPlayhead(map)
    }

    private fun beginRecord() {
        val map = _ui.value.selected ?: return
        taps.clear()
        playheadJob?.cancel()
        _ui.update { it.copy(phase = ImitatePhase.Record, playheadBeat = 0.0) }
        // Silent timeline of same length so the playhead still guides pacing.
        engine.play(map, muted = true)
        startPlayhead(map)
    }

    /** Smooth playhead from audible origin (same clock as clicks), not per-note jumps. */
    private fun startPlayhead(map: Beatmap) {
        playheadJob?.cancel()
        playheadJob = viewModelScope.launch {
            val beatMs = 60_000.0 / map.bpm.coerceAtLeast(1)
            val endBeat = map.durationBeats.coerceAtLeast(0.0)
            while (isActive) {
                val phase = _ui.value.phase
                if (phase != ImitatePhase.Demo && phase != ImitatePhase.Record) break
                val origin = engine.audibleOriginUptimeMs
                if (origin > 0L) {
                    val elapsed = SystemClock.uptimeMillis() - origin
                    val beat = (elapsed / beatMs).coerceIn(0.0, endBeat)
                    _ui.update { it.copy(playheadBeat = beat) }
                }
                delay(16)
            }
        }
    }

    fun onTap(eventTimeMs: Long) {
        if (_ui.value.phase != ImitatePhase.Record) return
        taps.add(eventTimeMs)
    }

    private fun finishRecord() {
        playheadJob?.cancel()
        val map = _ui.value.selected ?: return
        // Relative rhythm: first tap = first note, overall tempo stretch ignored.
        val expected = map.expectedTimesMs(0L)
        val score = Judge.evaluateRelative(
            expected,
            applyInputOffset(taps.toList(), timing.inputOffsetMs)
        )
        _ui.update { it.copy(phase = ImitatePhase.Result, score = score, playheadBeat = null) }
    }

    fun stop() {
        playheadJob?.cancel()
        engine.stop()
        if (_ui.value.phase == ImitatePhase.Record) finishRecord()
        else _ui.update { it.copy(phase = ImitatePhase.Pick, playheadBeat = null) }
    }

    override fun onCleared() {
        playheadJob?.cancel()
        engine.stop()
        engine.onEventAudible = null
        engine.onStarted = null
        engine.onCompleted = null
        super.onCleared()
    }
}
