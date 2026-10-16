package com.tickbeat.app.ui.sightread

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
import com.tickbeat.app.ui.common.VexRhythmView
import com.tickbeat.app.ui.common.ScoreReportCard
import com.tickbeat.app.ui.common.TapPad

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SightReadScreen(
    viewModel: SightReadViewModel = viewModel()
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.maps.forEach { item ->
                FilterChip(
                    selected = item.id == map?.id,
                    enabled = state.phase != SightPhase.Playing,
                    onClick = { viewModel.select(item) },
                    label = { Text(item.title.substringAfter("· ").ifEmpty { item.title }) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        FilterChip(
            selected = state.clickEnabled,
            onClick = viewModel::toggleClick,
            enabled = state.phase != SightPhase.Playing,
            label = { Text(if (state.clickEnabled) "弱拍提示：开" else "弱拍提示：关") }
        )

        Spacer(Modifier.height(16.dp))
        if (map != null) {
            Text(map.title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            VexRhythmView(
                beatmap = map,
                playheadBeat = state.playheadBeat,
                score = state.score
            )
        }

        Spacer(Modifier.height(16.dp))
        TapPad(
            enabled = state.phase == SightPhase.Playing,
            label = if (state.phase == SightPhase.Playing) "按谱打拍" else "开始后打拍",
            onTap = viewModel::onTap
        )

        Spacer(Modifier.height(12.dp))
        when (state.phase) {
            SightPhase.Pick, SightPhase.Result -> {
                TextButton(onClick = viewModel::start) { Text("开始视奏") }
            }
            SightPhase.Playing -> {
                TextButton(onClick = viewModel::stop) { Text("结束") }
            }
        }

        state.score?.let {
            Spacer(Modifier.height(16.dp))
            ScoreReportCard(score = it, onRetry = viewModel::start)
        }
    }
}
