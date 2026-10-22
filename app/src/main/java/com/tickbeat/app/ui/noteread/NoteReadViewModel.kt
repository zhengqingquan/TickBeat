package com.tickbeat.app.ui.noteread

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tickbeat.app.audio.TonePlaybackEngine
import com.tickbeat.app.domain.NoteRange
import com.tickbeat.app.domain.NoteReadFactory
import com.tickbeat.app.domain.NoteReadQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class NoteReadPhase { Ready, Answered }

data class NoteReadUiState(
    val question: NoteReadQuestion? = null,
    val phase: NoteReadPhase = NoteReadPhase.Ready,
    val selectedMidi: Int? = null,
    val correct: Boolean? = null,
    val streak: Int = 0,
    val answeredCount: Int = 0,
    val correctCount: Int = 0,
    val range: NoteRange = NoteRange.C4C5,
    val allowAccidentals: Boolean = true
)

class NoteReadViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = TonePlaybackEngine()
    private var round = 0

    private val _ui = MutableStateFlow(NoteReadUiState())
    val ui: StateFlow<NoteReadUiState> = _ui.asStateFlow()

    init {
        nextQuestion(play = false)
    }

    fun selectRange(range: NoteRange) {
        if (range == _ui.value.range) return
        _ui.update { it.copy(range = range) }
        nextQuestion(play = false)
    }

    fun toggleAccidentals() {
        _ui.update { it.copy(allowAccidentals = !it.allowAccidentals) }
        nextQuestion(play = false)
    }

    fun nextQuestion(play: Boolean = false) {
        engine.stop()
        round++
        val state = _ui.value
        val q = NoteReadFactory.random(
            seed = round * 41 + 11,
            range = state.range,
            allowAccidentals = state.allowAccidentals
        )
        _ui.update {
            it.copy(
                question = q,
                phase = NoteReadPhase.Ready,
                selectedMidi = null,
                correct = null
            )
        }
        if (play) {
            engine.playNote(q.midi)
        }
    }

    fun onKey(midi: Int) {
        engine.playNote(midi)
        val q = _ui.value.question ?: return
        if (_ui.value.phase == NoteReadPhase.Answered) return

        val ok = midi == q.midi
        _ui.update {
            it.copy(
                phase = NoteReadPhase.Answered,
                selectedMidi = midi,
                correct = ok,
                answeredCount = it.answeredCount + 1,
                correctCount = it.correctCount + if (ok) 1 else 0,
                streak = if (ok) it.streak + 1 else 0
            )
        }
    }

    fun stop() {
        engine.stop()
    }

    override fun onCleared() {
        engine.stop()
        super.onCleared()
    }
}
