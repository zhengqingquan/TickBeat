package com.tickbeat.app.ui.noteread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickbeat.app.domain.IntervalQuestionFactory
import com.tickbeat.app.domain.NoteClef
import com.tickbeat.app.domain.NoteRange
import com.tickbeat.app.ui.common.NoteStaffView
import com.tickbeat.app.ui.common.PianoKeyboard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteReadScreen(
    viewModel: NoteReadViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val q = state.question
    val ranges = NoteRange.forClef(state.clef)

    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "正确 ${state.correctCount}/${state.answeredCount} · 连对 ${state.streak}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "谱号 / 音域",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = state.fullRandom,
                onClick = viewModel::toggleFullRandom,
                label = { Text("全随机") }
            )
            NoteClef.entries.forEach { clef ->
                FilterChip(
                    selected = !state.fullRandom && clef == state.clef,
                    onClick = { viewModel.selectClef(clef) },
                    label = { Text(clef.labelZh) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // In full-random, show all ranges unselected; otherwise clef-filtered.
            val rangeChoices = if (state.fullRandom) NoteRange.entries else ranges
            rangeChoices.forEach { range ->
                FilterChip(
                    selected = !state.fullRandom && range == state.range,
                    onClick = { viewModel.selectRange(range) },
                    label = { Text(range.labelZh) }
                )
            }
            FilterChip(
                selected = state.allowAccidentals,
                onClick = viewModel::toggleAccidentals,
                label = { Text("升降号") }
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "弹出谱上的音",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        if (q != null) {
            NoteStaffView(
                midi = q.midi,
                clef = state.clef,
                correct = if (state.phase == NoteReadPhase.Answered) state.correct else null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.phase == NoteReadPhase.Answered) {
            Spacer(Modifier.height(12.dp))
            val answerName = q?.let { IntervalQuestionFactory.midiName(it.midi) }.orEmpty()
            Text(
                text = if (state.correct == true) {
                    "回答正确 · $answerName"
                } else {
                    "再试：正解是 $answerName"
                },
                color = if (state.correct == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.nextQuestion(play = false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("下一题")
            }
        }

        Spacer(Modifier.weight(1f))
        PianoKeyboard(
            onNoteDown = viewModel::onKey,
            lowMidi = state.range.lowMidi,
            highMidi = state.range.highMidi,
            highlightMidi = if (state.phase == NoteReadPhase.Answered) q?.midi else null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "看谱，在键盘上弹出对应的音",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
