package com.tickbeat.app.ui.sightread

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tickbeat.app.audio.RhythmPlaybackEngine
import com.tickbeat.app.data.BeatmapRepository
import com.tickbeat.app.domain.Beatmap
import com.tickbeat.app.domain.Judge
import com.tickbeat.app.domain.SessionScore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SightPhase { Pick, Playing, Result }

data class SightUiState(
    val maps: List<Beatmap> = emptyList(),
    val selected: Beatmap? = null,
    val phase: SightPhase = SightPhase.Pick,
    val playheadBeat: Double = 0.0,
    val score: SessionScore? = null,
    val clickEnabled: Boolean = true
)

class SightReadViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BeatmapRepository.get(app)
    private val engine = RhythmPlaybackEngine()

    private val _ui = MutableStateFlow(SightUiState())
    val ui: StateFlow<SightUiState> = _ui.asStateFlow()

    private val taps = mutableListOf<Long>()
    private var playheadJob: Job? = null

    init {
        val maps = repo.byTag("sightread")
        _ui.update { it.copy(maps = maps, selected = maps.firstOrNull()) }
        engine.onCompleted = { finish() }
    }

    fun select(map: Beatmap) {
        if (_ui.value.phase == SightPhase.Playing) return
        _ui.update { it.copy(selected = map, score = null, playheadBeat = 0.0) }
    }

    fun toggleClick() {
        _ui.update { it.copy(clickEnabled = !it.clickEnabled) }
    }

    fun start() {
        val map = _ui.value.selected ?: return
        taps.clear()
        _ui.update { it.copy(phase = SightPhase.Playing, score = null, playheadBeat = 0.0) }
        engine.play(map, muted = !_ui.value.clickEnabled)
        playheadJob?.cancel()
        playheadJob = viewModelScope.launch {
            val beatMs = 60_000.0 / map.bpm
            while (isActive && _ui.value.phase == SightPhase.Playing) {
                val origin = engine.audibleOriginUptimeMs
                if (origin > 0L) {
                    val elapsed = SystemClock.uptimeMillis() - origin
                    val beat = (elapsed / beatMs).coerceAtLeast(0.0)
                    _ui.update { it.copy(playheadBeat = beat) }
                }
                delay(16)
            }
        }
    }

    fun onTap(eventTimeMs: Long) {
        if (_ui.value.phase != SightPhase.Playing) return
        taps.add(eventTimeMs)
    }

    fun stop() {
        engine.stop()
        if (_ui.value.phase == SightPhase.Playing) finish()
    }

    private fun finish() {
        playheadJob?.cancel()
        val map = _ui.value.selected ?: return
        val expected = map.expectedTimesMs(engine.audibleOriginUptimeMs)
        val score = Judge.evaluate(expected, taps.toList())
        _ui.update { it.copy(phase = SightPhase.Result, score = score) }
    }

    override fun onCleared() {
        playheadJob?.cancel()
        engine.stop()
        engine.onCompleted = null
        super.onCleared()
    }
}
