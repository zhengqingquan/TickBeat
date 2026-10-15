package com.tickbeat.app.ui.eartrain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

@Composable
fun EarTrainScreen(
    viewModel: EarTrainViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val q = state.question

    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "正确 ${state.correctCount}/${state.answeredCount} · 连对 ${state.streak}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = q?.prompt ?: "暂无题目",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = q?.base?.title ?: "",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::playCorrupt,
            enabled = state.phase != EarPhase.Playing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("播放题目节奏")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = viewModel::playCorrectReference,
            enabled = state.phase != EarPhase.Playing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("对比：播放正确版")
        }

        Spacer(Modifier.height(20.dp))
        q?.choices?.forEachIndexed { index, label ->
            val selected = state.selectedChoice == index
            val isCorrectChoice = q.correctChoiceIndex == index
            val answered = state.phase == EarPhase.Answered
            OutlinedButton(
                onClick = { viewModel.answer(index) },
                enabled = state.phase != EarPhase.Playing && !answered,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = buildString {
                        append(label)
                        if (answered && selected) append(if (state.correct == true) "  ✓" else "  ✗")
                        if (answered && isCorrectChoice && state.correct == false) append("  (正解)")
                    }
                )
            }
        }

        if (state.phase == EarPhase.Answered) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (state.correct == true) "回答正确" else "再听一遍对照正解",
                color = if (state.correct == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = viewModel::nextQuestion,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("下一题")
            }
        }
    }
}
