package com.tickbeat.app.ui.sightread

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tickbeat.app.data.BeatmapRepository
import com.tickbeat.app.data.TimingPreferences
import com.tickbeat.app.domain.Beatmap
import com.tickbeat.app.domain.Judge
import com.tickbeat.app.domain.SessionScore
import com.tickbeat.app.domain.applyInputOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SightPhase { Ready, Recording, Result }

data class SightUiState(
    val maps: List<Beatmap> = emptyList(),
    val selected: Beatmap? = null,
    val phase: SightPhase = SightPhase.Ready,
    val tapCount: Int = 0,
    val expectedCount: Int = 0,
    val score: SessionScore? = null
)

/**
 * Sight-read: look at the notation and tap at your own tempo.
 * First tap starts recording; scoring uses [Judge.evaluateRelative]
 * (relative intervals, not wall-clock sync to a click/playhead).
 */
class SightReadViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BeatmapRepository.get(app)
    private val timing = TimingPreferences.get(app)

    private val _ui = MutableStateFlow(SightUiState())
    val ui: StateFlow<SightUiState> = _ui.asStateFlow()

    private val taps = mutableListOf<Long>()

    init {
        val maps = repo.byTag("sightread")
        val first = maps.firstOrNull()
        _ui.update {
            it.copy(
                maps = maps,
                selected = first,
                expectedCount = first?.events?.size ?: 0
            )
        }
    }

    fun select(map: Beatmap) {
        if (_ui.value.phase == SightPhase.Recording) return
        taps.clear()
        _ui.update {
            it.copy(
                selected = map,
                score = null,
                phase = SightPhase.Ready,
                tapCount = 0,
                expectedCount = map.events.size
            )
        }
    }

    fun onTap(eventTimeMs: Long) {
        val state = _ui.value
        if (state.selected == null) return
        if (state.phase == SightPhase.Result) return

        if (state.phase == SightPhase.Ready) {
            taps.clear()
            _ui.update { it.copy(phase = SightPhase.Recording, score = null, tapCount = 0) }
        }

        taps.add(eventTimeMs)
        val count = taps.size
        val expected = state.expectedCount
        _ui.update { it.copy(tapCount = count) }

        // Enough taps for the pattern → auto finish (own tempo, no playhead).
        if (expected > 0 && count >= expected) {
            finish()
        }
    }

    fun finish() {
        if (_ui.value.phase != SightPhase.Recording) return
        val map = _ui.value.selected ?: return
        val expected = map.expectedTimesMs(0L)
        val score = Judge.evaluateRelative(
            expected,
            applyInputOffset(taps.toList(), timing.inputOffsetMs)
        )
        _ui.update {
            it.copy(phase = SightPhase.Result, score = score)
        }
    }

    fun reset() {
        taps.clear()
        _ui.update {
            it.copy(
                phase = SightPhase.Ready,
                score = null,
                tapCount = 0
            )
        }
    }

    /** Leave page / hide: discard in-progress taps without scoring. */
    fun stop() {
        if (_ui.value.phase == SightPhase.Recording) {
            taps.clear()
            _ui.update { it.copy(phase = SightPhase.Ready, tapCount = 0) }
        }
    }
}
