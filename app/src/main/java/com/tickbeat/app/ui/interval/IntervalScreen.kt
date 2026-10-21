package com.tickbeat.app.ui.interval

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
import androidx.compose.material3.OutlinedButton
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
import com.tickbeat.app.domain.IntervalQuality
import com.tickbeat.app.domain.IntervalQuestionFactory
import com.tickbeat.app.ui.common.PianoKeyboard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntervalScreen(
    viewModel: IntervalViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val q = state.question

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
            text = "题目范围",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            IntervalQuality.entries.forEach { quality ->
                FilterChip(
                    selected = quality in state.qualities,
                    onClick = { viewModel.toggleQuality(quality) },
                    label = { Text(quality.labelZh) }
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
            text = q?.prompt ?: "暂无题目",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = q?.let { "根音 ${IntervalQuestionFactory.midiName(it.rootMidi)}" }.orEmpty(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = viewModel::playRoot,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("播放根音")
        }

        if (state.phase == IntervalPhase.Answered) {
            Spacer(Modifier.height(12.dp))
            val answerName = q?.let { IntervalQuestionFactory.midiName(it.answerMidi) }.orEmpty()
            val semitones = q?.interval?.semitones
            val semiLabel = semitones?.let { "（$it 个半音）" }.orEmpty()
            Text(
                text = if (state.correct == true) {
                    "回答正确 · $answerName$semiLabel"
                } else {
                    "再试：正解是 $answerName$semiLabel"
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
                onClick = { viewModel.nextQuestion() },
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
            highlightMidi = if (state.phase == IntervalPhase.Answered) q?.answerMidi else null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "在键盘上弹出目标音",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
