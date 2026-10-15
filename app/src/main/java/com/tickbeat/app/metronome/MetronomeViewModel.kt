package com.tickbeat.app.metronome

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MetronomeUiState(
    val bpm: Int = 120,
    val timeSignature: TimeSignature = TimeSignature.Default,
    val isPlaying: Boolean = false
) {
    val beatsPerBar: Int get() = timeSignature.beats
}

class MetronomeViewModel(
    private val engine: MetronomeEngine = MetronomeEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetronomeUiState())
    val uiState: StateFlow<MetronomeUiState> = _uiState.asStateFlow()

    /** High-frequency beat index — kept off [uiState] so the main chrome doesn't recompose every click. */
    private val _currentBeat = MutableStateFlow(0)
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    init {
        engine.onBeat = { beat ->
            _currentBeat.value = beat
        }
    }

    fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(30, 300)
        engine.setBpm(clamped)
        _uiState.update { it.copy(bpm = clamped) }
    }

    fun adjustBpm(delta: Int) = setBpm(_uiState.value.bpm + delta)

    fun setTimeSignature(signature: TimeSignature) {
        val safe = TimeSignature.sanitize(signature.beats, signature.noteValue)
        engine.setBeatsPerBar(safe.beats)
        _currentBeat.value = 0
        _uiState.update { it.copy(timeSignature = safe) }
    }

    fun setBeats(beats: Int) {
        val current = _uiState.value.timeSignature
        setTimeSignature(current.copy(beats = beats))
    }

    fun setNoteValue(noteValue: Int) {
        val current = _uiState.value.timeSignature
        setTimeSignature(current.copy(noteValue = noteValue))
    }

    fun togglePlaying() {
        if (_uiState.value.isPlaying) {
            engine.stop()
            _currentBeat.value = 0
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            engine.setBpm(_uiState.value.bpm)
            engine.setBeatsPerBar(_uiState.value.beatsPerBar)
            engine.start()
            _uiState.update { it.copy(isPlaying = true) }
        }
    }

    override fun onCleared() {
        engine.stop()
        engine.onBeat = null
        super.onCleared()
    }
}
