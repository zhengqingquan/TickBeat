package com.tickbeat.app.metronome

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MetronomeUiState(
    val bpm: Int = 120,
    val beatsPerBar: Int = 4,
    val isPlaying: Boolean = false,
    val currentBeat: Int = 0
)

class MetronomeViewModel(
    private val engine: MetronomeEngine = MetronomeEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetronomeUiState())
    val uiState: StateFlow<MetronomeUiState> = _uiState.asStateFlow()

    init {
        engine.onBeat = { beat ->
            _uiState.update { it.copy(currentBeat = beat) }
        }
    }

    fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(30, 300)
        engine.setBpm(clamped)
        _uiState.update { it.copy(bpm = clamped) }
    }

    fun adjustBpm(delta: Int) = setBpm(_uiState.value.bpm + delta)

    fun setBeatsPerBar(beats: Int) {
        val clamped = beats.coerceIn(1, 12)
        engine.setBeatsPerBar(clamped)
        _uiState.update { it.copy(beatsPerBar = clamped, currentBeat = 0) }
    }

    fun togglePlaying() {
        if (_uiState.value.isPlaying) {
            engine.stop()
            _uiState.update { it.copy(isPlaying = false, currentBeat = 0) }
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
