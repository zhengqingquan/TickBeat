package com.tickbeat.app.ui.follow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tickbeat.app.audio.RhythmPlaybackEngine
import com.tickbeat.app.data.BeatmapRepository
import com.tickbeat.app.data.TimingPreferences
import com.tickbeat.app.domain.Beatmap
import com.tickbeat.app.domain.HitGrade
import com.tickbeat.app.domain.Judge
import com.tickbeat.app.domain.JudgeConfig
import com.tickbeat.app.domain.SessionScore
import com.tickbeat.app.domain.applyInputOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FollowPhase {
    Idle,
    Playing,
    Finished
}

data class FollowUiState(
    val maps: List<Beatmap> = emptyList(),
    val selected: Beatmap? = null,
    val phase: FollowPhase = FollowPhase.Idle,
    val lastGrade: HitGrade? = null,
    val liveCombo: Int = 0,
    val score: SessionScore? = null,
    val eventIndex: Int = -1
)

class FollowViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BeatmapRepository.get(app)
    private val timing = TimingPreferences.get(app)
    private val engine = RhythmPlaybackEngine()

    private val _ui = MutableStateFlow(FollowUiState())
    val ui: StateFlow<FollowUiState> = _ui.asStateFlow()

    private val taps = mutableListOf<Long>()
    private var expectedAudible = listOf<Long>()

    init {
        val maps = repo.byTag("follow")
        _ui.update { it.copy(maps = maps, selected = maps.firstOrNull()) }
        engine.onEventAudible = { index, _ ->
            _ui.update { it.copy(eventIndex = index) }
        }
        engine.onStarted = { origin ->
            val map = _ui.value.selected
            if (map != null) {
                expectedAudible = map.expectedTimesMs(origin)
            }
        }
        engine.onCompleted = {
            finishSession()
        }
    }

    fun select(map: Beatmap) {
        if (_ui.value.phase == FollowPhase.Playing) return
        _ui.update { it.copy(selected = map, score = null, lastGrade = null, liveCombo = 0) }
    }

    fun start() {
        val map = _ui.value.selected ?: return
        taps.clear()
        expectedAudible = emptyList()
        _ui.update {
            it.copy(
                phase = FollowPhase.Playing,
                score = null,
                lastGrade = null,
                liveCombo = 0,
                eventIndex = -1
            )
        }
        engine.play(map, muted = false)
    }

    fun onTap(eventTimeMs: Long) {
        if (_ui.value.phase != FollowPhase.Playing) return
        taps.add(eventTimeMs)
        val expected = expectedAudible
        if (expected.isEmpty()) return
        val adjusted = applyInputOffset(eventTimeMs, timing.inputOffsetMs)
        val nearest = expected.minByOrNull { kotlin.math.abs(it - adjusted) } ?: return
        val err = kotlin.math.abs(nearest - adjusted)
        val grade = when {
            err <= JudgeConfig.PerfectWindowMs -> HitGrade.Perfect
            err <= JudgeConfig.GoodWindowMs -> HitGrade.Good
            else -> HitGrade.Miss
        }
        _ui.update {
            val combo = if (grade == HitGrade.Miss) 0 else it.liveCombo + 1
            it.copy(lastGrade = grade, liveCombo = combo)
        }
    }

    fun stop() {
        engine.stop()
        if (_ui.value.phase == FollowPhase.Playing) finishSession()
    }

    private fun finishSession() {
        if (_ui.value.phase == FollowPhase.Finished) return
        val map = _ui.value.selected ?: return
        val expected = expectedAudible.ifEmpty {
            map.expectedTimesMs(engine.audibleOriginUptimeMs)
        }
        val score = Judge.evaluate(
            expected,
            applyInputOffset(taps.toList(), timing.inputOffsetMs)
        )
        _ui.update { it.copy(phase = FollowPhase.Finished, score = score) }
    }

    override fun onCleared() {
        engine.stop()
        engine.onEventAudible = null
        engine.onStarted = null
        engine.onCompleted = null
        super.onCleared()
    }
}
