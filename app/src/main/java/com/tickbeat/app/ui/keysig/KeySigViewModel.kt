package com.tickbeat.app.ui.keysig

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tickbeat.app.domain.KeyMode
import com.tickbeat.app.domain.KeySigFactory
import com.tickbeat.app.domain.KeySigQuestion
import com.tickbeat.app.domain.MusicKey
import com.tickbeat.app.domain.NoteClef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class KeySigPhase { Ready, Answered }

data class KeySigUiState(
    val question: KeySigQuestion? = null,
    val phase: KeySigPhase = KeySigPhase.Ready,
    val selected: MusicKey? = null,
    val correct: Boolean? = null,
    val streak: Int = 0,
    val answeredCount: Int = 0,
    val correctCount: Int = 0,
    val clef: NoteClef = NoteClef.Treble,
    val modes: Set<KeyMode> = setOf(KeyMode.Major, KeyMode.Minor)
)

class KeySigViewModel(app: Application) : AndroidViewModel(app) {
    private var round = 0

    private val _ui = MutableStateFlow(KeySigUiState())
    val ui: StateFlow<KeySigUiState> = _ui.asStateFlow()

    init {
        nextQuestion()
    }

    fun selectClef(clef: NoteClef) {
        _ui.update { it.copy(clef = clef) }
        nextQuestion()
    }

    fun toggleMode(mode: KeyMode) {
        val current = _ui.value.modes
        val next = if (mode in current) {
            if (current.size <= 1) current else current - mode
        } else {
            current + mode
        }
        _ui.update { it.copy(modes = next) }
        nextQuestion()
    }

    fun nextQuestion() {
        round++
        val modes = _ui.value.modes
        val q = KeySigFactory.random(seed = round * 37 + 19, modes = modes)
        _ui.update {
            it.copy(
                question = q,
                phase = KeySigPhase.Ready,
                selected = null,
                correct = null
            )
        }
    }

    fun answer(choice: MusicKey) {
        val q = _ui.value.question ?: return
        if (_ui.value.phase == KeySigPhase.Answered) return
        val ok = choice.id == q.answer.id
        _ui.update {
            it.copy(
                phase = KeySigPhase.Answered,
                selected = choice,
                correct = ok,
                answeredCount = it.answeredCount + 1,
                correctCount = it.correctCount + if (ok) 1 else 0,
                streak = if (ok) it.streak + 1 else 0
            )
        }
    }

    fun stop() {
        // No audio engine.
    }
}
