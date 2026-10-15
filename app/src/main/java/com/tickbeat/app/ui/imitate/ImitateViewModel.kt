package com.tickbeat.app.ui.imitate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tickbeat.app.audio.RhythmPlaybackEngine
import com.tickbeat.app.data.BeatmapRepository
import com.tickbeat.app.domain.Beatmap
import com.tickbeat.app.domain.Judge
import com.tickbeat.app.domain.SessionScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    private val repo = BeatmapRepository(app)
    private val engine = RhythmPlaybackEngine()

    private val _ui = MutableStateFlow(ImitateUiState())
    val ui: StateFlow<ImitateUiState> = _ui.asStateFlow()

    private val taps = mutableListOf<Long>()
    private var recordOrigin = 0L
    private var demoCountTarget = 2

    init {
        val maps = repo.byTag("imitate")
        _ui.update { it.copy(maps = maps, selected = maps.firstOrNull()) }
        engine.onEventAudible = { index, _ ->
            val map = _ui.value.selected
            val t = map?.events?.getOrNull(index)?.tBeat
            if (t != null) {
                _ui.update { it.copy(playheadBeat = t) }
            }
        }
        engine.onCompleted = {
            when (_ui.value.phase) {
                ImitatePhase.Demo -> {
                    val next = _ui.value.demoPass + 1
                    if (next < demoCountTarget) {
                        _ui.update { it.copy(demoPass = next) }
                        _ui.value.selected?.let { engine.play(it) }
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
        _ui.update {
            it.copy(phase = ImitatePhase.Demo, demoPass = 0, score = null, playheadBeat = 0.0)
        }
        engine.play(map)
    }

    private fun beginRecord() {
        val map = _ui.value.selected ?: return
        taps.clear()
        recordOrigin = 0L
        _ui.update { it.copy(phase = ImitatePhase.Record, playheadBeat = 0.0) }
        engine.onStarted = { origin -> recordOrigin = origin }
        // Silent timeline of same length so user has a window.
        engine.play(map, muted = true)
    }

    fun onTap(eventTimeMs: Long) {
        if (_ui.value.phase != ImitatePhase.Record) return
        if (recordOrigin == 0L) {
            recordOrigin = engine.audibleOriginUptimeMs
        }
        taps.add(eventTimeMs)
    }

    private fun finishRecord() {
        val map = _ui.value.selected ?: return
        val origin = engine.audibleOriginUptimeMs.takeIf { it > 0 } ?: recordOrigin
        val expected = map.expectedTimesMs(origin)
        val score = Judge.evaluate(expected, taps.toList())
        _ui.update { it.copy(phase = ImitatePhase.Result, score = score, playheadBeat = null) }
    }

    fun stop() {
        engine.stop()
        if (_ui.value.phase == ImitatePhase.Record) finishRecord()
        else _ui.update { it.copy(phase = ImitatePhase.Pick, playheadBeat = null) }
    }

    override fun onCleared() {
        engine.stop()
        engine.onEventAudible = null
        engine.onStarted = null
        engine.onCompleted = null
        super.onCleared()
    }
}
