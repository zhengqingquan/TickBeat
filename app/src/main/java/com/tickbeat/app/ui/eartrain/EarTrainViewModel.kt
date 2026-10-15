package com.tickbeat.app.ui.eartrain

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tickbeat.app.audio.RhythmPlaybackEngine
import com.tickbeat.app.data.BeatmapRepository
import com.tickbeat.app.domain.EarQuestion
import com.tickbeat.app.domain.EarQuestionFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class EarPhase { Ready, Playing, Answered }

data class EarUiState(
    val question: EarQuestion? = null,
    val phase: EarPhase = EarPhase.Ready,
    val selectedChoice: Int? = null,
    val correct: Boolean? = null,
    val streak: Int = 0,
    val answeredCount: Int = 0,
    val correctCount: Int = 0
)

class EarTrainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BeatmapRepository(app)
    private val engine = RhythmPlaybackEngine()
    private val pool = repo.byTag("ear").ifEmpty { repo.byTag("imitate") }

    private var round = 0

    private val _ui = MutableStateFlow(EarUiState())
    val ui: StateFlow<EarUiState> = _ui.asStateFlow()

    init {
        nextQuestion()
        engine.onCompleted = {
            _ui.update { state ->
                if (state.phase == EarPhase.Playing) state.copy(phase = EarPhase.Ready) else state
            }
        }
    }

    fun nextQuestion() {
        engine.stop()
        if (pool.isEmpty()) return
        val base = pool[round % pool.size]
        round++
        val q = EarQuestionFactory.fromBeatmap(base, seed = base.id.hashCode() + round * 17)
        _ui.update {
            it.copy(
                question = q,
                phase = EarPhase.Ready,
                selectedChoice = null,
                correct = null
            )
        }
    }

    fun playCorrupt() {
        val q = _ui.value.question ?: return
        _ui.update { it.copy(phase = EarPhase.Playing) }
        engine.play(q.corrupt)
    }

    fun playCorrectReference() {
        val q = _ui.value.question ?: return
        _ui.update { it.copy(phase = EarPhase.Playing) }
        engine.play(q.base)
    }

    fun answer(choiceIndex: Int) {
        val q = _ui.value.question ?: return
        if (_ui.value.phase == EarPhase.Answered) return
        val ok = choiceIndex == q.correctChoiceIndex
        _ui.update {
            it.copy(
                phase = EarPhase.Answered,
                selectedChoice = choiceIndex,
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
        engine.onCompleted = null
        super.onCleared()
    }
}
