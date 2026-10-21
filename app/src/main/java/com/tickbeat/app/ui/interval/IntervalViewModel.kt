package com.tickbeat.app.ui.interval

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tickbeat.app.audio.TonePlaybackEngine
import com.tickbeat.app.domain.IntervalQuestion
import com.tickbeat.app.domain.IntervalQuestionFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class IntervalPhase { Ready, Answered }

data class IntervalUiState(
    val question: IntervalQuestion? = null,
    val phase: IntervalPhase = IntervalPhase.Ready,
    val selectedMidi: Int? = null,
    val correct: Boolean? = null,
    val streak: Int = 0,
    val answeredCount: Int = 0,
    val correctCount: Int = 0
)

class IntervalViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = TonePlaybackEngine()
    private var round = 0

    private val _ui = MutableStateFlow(IntervalUiState())
    val ui: StateFlow<IntervalUiState> = _ui.asStateFlow()

    init {
        nextQuestion(playRoot = true)
    }

    fun nextQuestion(playRoot: Boolean = true) {
        engine.stop()
        round++
        val q = IntervalQuestionFactory.random(seed = round * 31 + 7)
        _ui.update {
            it.copy(
                question = q,
                phase = IntervalPhase.Ready,
                selectedMidi = null,
                correct = null
            )
        }
        if (playRoot) {
            engine.playNote(q.rootMidi)
        }
    }

    fun playRoot() {
        val q = _ui.value.question ?: return
        engine.playNote(q.rootMidi)
    }

    fun onKey(midi: Int) {
        engine.playNote(midi)
        val q = _ui.value.question ?: return
        if (_ui.value.phase == IntervalPhase.Answered) return

        val ok = midi == q.answerMidi
        _ui.update {
            it.copy(
                phase = IntervalPhase.Answered,
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
