package com.tickbeat.app.ui.sightread

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickbeat.app.domain.Beatmap
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
            RhythmNotationView(
                beatmap = map,
                playheadBeat = state.playheadBeat
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

@Composable
private fun RhythmNotationView(
    beatmap: Beatmap,
    playheadBeat: Double
) {
    val barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val noteColor = MaterialTheme.colorScheme.primary
    val headColor = MaterialTheme.colorScheme.secondary
    val duration = beatmap.durationBeats.coerceAtLeast(1.0)
    val beatsPerBar = beatmap.beatsPerBar.coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val laneY = size.height * 0.55f
        val noteH = size.height * 0.28f
        drawLine(barColor, Offset(0f, laneY), Offset(size.width, laneY), strokeWidth = 2f)

        val bars = (duration / beatsPerBar).toInt().coerceAtLeast(1)
        for (b in 0..bars) {
            val x = ((b * beatsPerBar) / duration * size.width).toFloat()
            drawLine(barColor, Offset(x, 12f), Offset(x, size.height - 12f), strokeWidth = 2f)
        }

        val unitW = (size.width / duration).toFloat()
        beatmap.events.forEach { event ->
            val x = (event.tBeat / duration * size.width).toFloat()
            val w = (0.4f * unitW).coerceAtLeast(10f)
            drawRoundRect(
                color = noteColor,
                topLeft = Offset(x, laneY - noteH),
                size = Size(w, noteH),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }

        val headX = ((playheadBeat / duration) * size.width).toFloat().coerceIn(0f, size.width)
        drawLine(headColor, Offset(headX, 8f), Offset(headX, size.height - 8f), strokeWidth = 4f)
    }
}
