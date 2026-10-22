package com.tickbeat.app.ui.keysig

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickbeat.app.domain.KeyMode
import com.tickbeat.app.domain.NoteClef
import com.tickbeat.app.ui.common.KeySigStaffView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeySigScreen(
    viewModel: KeySigViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val q = state.question

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
            text = "谱号",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            NoteClef.entries.forEach { clef ->
                FilterChip(
                    selected = clef == state.clef,
                    onClick = { viewModel.selectClef(clef) },
                    label = { Text(clef.labelZh) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
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
            KeyMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode in state.modes,
                    onClick = { viewModel.toggleMode(mode) },
                    label = { Text(mode.labelZh) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "这是什么调？",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        if (q != null) {
            KeySigStaffView(
                vexKey = q.answer.vexKey,
                clef = state.clef,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            q?.options?.forEach { option ->
                val answered = state.phase == KeySigPhase.Answered
                val isCorrect = option.id == q.answer.id
                val isSelected = state.selected?.id == option.id
                when {
                    answered && isCorrect -> Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(option.labelZh)
                    }
                    answered && isSelected -> Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            disabledContainerColor = MaterialTheme.colorScheme.error,
                            disabledContentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(option.labelZh)
                    }
                    else -> OutlinedButton(
                        onClick = { viewModel.answer(option) },
                        enabled = !answered,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(option.labelZh)
                    }
                }
            }
        }

        if (state.phase == KeySigPhase.Answered) {
            Spacer(Modifier.height(12.dp))
            val answerLabel = q?.answer?.labelZh.orEmpty()
            Text(
                text = if (state.correct == true) {
                    "回答正确 · $answerLabel"
                } else {
                    "再试：正解是 $answerLabel"
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
                onClick = viewModel::nextQuestion,
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
        Text(
            text = "看谱上的调号，选出对应的大调或关系小调",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
