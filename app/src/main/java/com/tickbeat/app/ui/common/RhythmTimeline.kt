package com.tickbeat.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tickbeat.app.domain.Beatmap
import com.tickbeat.app.domain.HitGrade
import com.tickbeat.app.domain.SessionScore

@Composable
fun RhythmTimeline(
    beatmap: Beatmap,
    playheadBeat: Double? = null,
    score: SessionScore? = null,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val perfect = Color(0xFF2E7D32)
    val good = Color(0xFFF9A825)
    val miss = Color(0xFFC62828)
    val playheadColor = MaterialTheme.colorScheme.secondary

    val duration = beatmap.durationBeats.coerceAtLeast(1.0)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val y = size.height / 2f
        drawLine(
            color = idle,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 3f
        )
        beatmap.events.forEachIndexed { index, event ->
            val x = ((event.tBeat / duration) * size.width).toFloat()
            val color = when (score?.results?.getOrNull(index)?.grade) {
                HitGrade.Perfect -> perfect
                HitGrade.Good -> good
                HitGrade.Miss -> miss
                null -> if (event.accent) primary else idle
            }
            val radius = if (event.accent) 14f else 10f
            drawCircle(color = color, radius = radius, center = Offset(x, y))
        }
        if (playheadBeat != null) {
            val x = ((playheadBeat / duration) * size.width).toFloat().coerceIn(0f, size.width)
            drawLine(
                color = playheadColor,
                start = Offset(x, 8f),
                end = Offset(x, size.height - 8f),
                strokeWidth = 4f
            )
        }
    }
}
