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
    /** Absolute beat where muted record bars begin (continuous session). */
    private var recordFromBeat = 0.0
    /** End of the last notated bar (exclusive). Past this, highlight must not wrap. */
    private var contentEndBeat = 0.0
    private var sessionEndBeat = 0.0

    init {
        val maps = repo.byTag("imitate")
        _ui.update { it.copy(maps = maps, selected = maps.firstOrNull()) }
        engine.onCompleted = {
            when (_ui.value.phase) {
                ImitatePhase.Demo, ImitatePhase.Record -> finishRecord()
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

        val barLen = map.beatsPerBar.toDouble().coerceAtLeast(1.0)
        // Continuous: N audible demo bars + 1 silent record bar, one AudioTrack session.
        val totalBars = demoCountTarget + 1
        recordFromBeat = demoCountTarget * barLen
        contentEndBeat = totalBars * barLen
        val session = map.repeatAcrossBars(totalBars)
        // Engine may run a short tail past the last bar; do not use that for highlight wrap.
        sessionEndBeat = session.durationBeats.coerceAtLeast(contentEndBeat)

        _ui.update {
            it.copy(phase = ImitatePhase.Demo, demoPass = 0, score = null, playheadBeat = 0.0)
        }
        engine.play(session, muteFromBeat = recordFromBeat)
        startPlayhead(map, barLen)
    }

    /** Smooth playhead; staff highlight uses beat within the written bar. */
    private fun startPlayhead(map: Beatmap, barLen: Double) {
        playheadJob?.cancel()
        playheadJob = viewModelScope.launch {
            val beatMs = 60_000.0 / map.bpm.coerceAtLeast(1)
            while (isActive) {
                val phase = _ui.value.phase
                if (phase != ImitatePhase.Demo && phase != ImitatePhase.Record) break
                val origin = engine.audibleOriginUptimeMs
                if (origin > 0L) {
                    val elapsed = SystemClock.uptimeMillis() - origin
                    val absBeat = (elapsed / beatMs).coerceIn(0.0, sessionEndBeat)
                    val inRecord = absBeat + 1e-9 >= recordFromBeat
                    // Tail after the last bar used to wrap via % and flash the rhythm again.
                    val displayBeat = if (absBeat + 1e-9 >= contentEndBeat) {
                        null
                    } else {
                        absBeat % barLen
                    }
                    val demoPass = if (inRecord) {
                        demoCountTarget - 1
                    } else {
                        (absBeat / barLen).toInt().coerceIn(0, demoCountTarget - 1)
                    }
                    _ui.update {
                        it.copy(
                            phase = if (inRecord) ImitatePhase.Record else ImitatePhase.Demo,
                            demoPass = demoPass,
                            playheadBeat = displayBeat
                        )
                    }
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
        // Absolute timing vs the silent record bar (same clock as the playhead).
        // Relative-first-tap anchoring falsely paints beat 1 green when the only
        // tap was actually on a later note.
        val beatMs = 60_000.0 / map.bpm.coerceAtLeast(1)
        val recordOriginMs = (engine.audibleOriginUptimeMs + recordFromBeat * beatMs).toLong()
        val expected = map.expectedTimesMs(recordOriginMs)
        val score = Judge.evaluate(
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
