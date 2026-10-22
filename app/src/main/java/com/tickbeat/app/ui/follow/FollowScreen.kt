package com.tickbeat.app.ui.follow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickbeat.app.domain.HitGrade
import com.tickbeat.app.ui.common.ScoreReportCard
import com.tickbeat.app.ui.common.TapPad

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FollowScreen(
    viewModel: FollowViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = state.selected?.title ?: "选择关卡",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.maps.forEach { map ->
                FilterChip(
                    selected = state.selected?.id == map.id,
                    onClick = { viewModel.select(map) },
                    enabled = state.phase != FollowPhase.Playing,
                    label = { Text("${map.bpm}") }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        val gradeText = when (state.lastGrade) {
            HitGrade.Perfect -> "Perfect"
            HitGrade.Good -> "Good"
            HitGrade.Miss -> "Miss"
            null -> "—"
        }
        Text(text = gradeText, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(text = "连击 ${state.liveCombo}")

        Spacer(Modifier.height(16.dp))

        TapPad(
            enabled = state.phase == FollowPhase.Playing,
            label = if (state.phase == FollowPhase.Playing) "跟着敲！" else "开始后在此打拍",
            onTap = viewModel::onTap
        )

        Spacer(Modifier.height(16.dp))

        when (state.phase) {
            FollowPhase.Idle, FollowPhase.Finished -> {
                Button(
                    onClick = viewModel::start,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (state.phase == FollowPhase.Finished) "再来一次" else "开始跟拍")
                }
            }
            FollowPhase.Playing -> {
                OutlinedButton(onClick = viewModel::stop) { Text("结束") }
            }
        }

        state.score?.let {
            Spacer(Modifier.height(16.dp))
            ScoreReportCard(score = it, onRetry = viewModel::start)
        }
    }
}
