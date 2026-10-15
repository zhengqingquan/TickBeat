package com.tickbeat.app.ui.imitate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickbeat.app.ui.common.RhythmTimeline
import com.tickbeat.app.ui.common.ScoreReportCard
import com.tickbeat.app.ui.common.TapPad

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImitateScreen(
    viewModel: ImitateViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val map = state.selected

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
            text = map?.title ?: "选择节奏",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.maps.forEach { item ->
                FilterChip(
                    selected = item.id == map?.id,
                    enabled = state.phase == ImitatePhase.Pick || state.phase == ImitatePhase.Result,
                    onClick = { viewModel.select(item) },
                    label = { Text(item.title.substringAfter("· ").ifEmpty { item.title }) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        if (map != null) {
            RhythmTimeline(
                beatmap = map,
                playheadBeat = state.playheadBeat,
                score = state.score
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = when (state.phase) {
                ImitatePhase.Pick -> "先听示范，再复打"
                ImitatePhase.Demo -> "示范中（${state.demoPass + 1}/2）"
                ImitatePhase.Record -> "请复打！"
                ImitatePhase.Result -> "结果"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))
        TapPad(
            enabled = state.phase == ImitatePhase.Record,
            label = if (state.phase == ImitatePhase.Record) "复打此处" else "等待复打阶段",
            onTap = viewModel::onTap
        )

        Spacer(Modifier.height(12.dp))
        when (state.phase) {
            ImitatePhase.Pick, ImitatePhase.Result -> {
                TextButton(onClick = viewModel::startDemo) { Text("开始") }
            }
            ImitatePhase.Demo, ImitatePhase.Record -> {
                TextButton(onClick = viewModel::stop) { Text("停止") }
            }
        }

        state.score?.let {
            Spacer(Modifier.height(16.dp))
            ScoreReportCard(score = it, onRetry = viewModel::startDemo)
        }
    }
}
