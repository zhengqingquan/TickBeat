package com.tickbeat.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import com.tickbeat.app.data.TimingPreferences
import com.tickbeat.app.ui.common.TapPad

@Composable
fun LatencySettingsScreen(
    viewModel: LatencySettingsViewModel = viewModel()
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()

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
            text = "判定延迟补偿",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "用于跟拍、节奏模仿、视奏。正值表示敲击偏晚（常见于音频更晚到达）。有线耳机更准，蓝牙误差可能较大。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = formatOffset(state.offsetMs),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Slider(
            value = state.offsetMs.toFloat(),
            onValueChange = { viewModel.setOffset(it.toInt()) },
            valueRange = TimingPreferences.MIN_OFFSET.toFloat()..TimingPreferences.MAX_OFFSET.toFloat(),
            steps = TimingPreferences.MAX_OFFSET - TimingPreferences.MIN_OFFSET - 1,
            enabled = state.phase != LatencyPhase.Calibrating,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${TimingPreferences.MIN_OFFSET} ms", style = MaterialTheme.typography.labelMedium)
            Text("${TimingPreferences.MAX_OFFSET} ms", style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = viewModel::resetOffset,
            enabled = state.phase != LatencyPhase.Calibrating,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("重置为 0")
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "听 click 校准",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "先听 ${state.countInBeats} 拍预备，再跟着敲 ${state.measureBeats} 拍。结束后取误差中位数作为建议补偿。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        when (state.phase) {
            LatencyPhase.Idle -> {
                Button(
                    onClick = viewModel::startCalibration,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text("开始校准")
                }
            }
            LatencyPhase.Calibrating -> {
                // Mutually exclusive: never show measure progress during count-in (avoids “拍 0/8”).
                val statusText = when {
                    state.beatIndex < 0 -> "预备…"
                    state.beatIndex < state.countInBeats ->
                        "预备 ${state.beatIndex + 1} / ${state.countInBeats}"
                    else -> {
                        val n = (state.beatIndex - state.countInBeats + 1)
                            .coerceIn(1, state.measureBeats)
                        "拍 $n / ${state.measureBeats}"
                    }
                }
                Text(text = statusText, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val measuring = state.beatIndex >= state.countInBeats
                val almostReady = state.beatIndex == state.countInBeats - 1
                TapPad(
                    // Accept input on the last prep beat (still gray); yellow only once measuring.
                    enabled = measuring || almostReady,
                    highlighted = measuring,
                    label = when {
                        state.beatIndex < 0 || state.beatIndex < state.countInBeats - 1 ->
                            "先听预备拍…"
                        almostReady -> "下一拍开始敲"
                        else -> "跟着 click 敲"
                    },
                    onTap = viewModel::onTap
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = viewModel::stopCalibration) {
                    Text("提前结束")
                }
            }
            LatencyPhase.Result -> {
                val measured = state.measuredMs
                if (measured == null) {
                    Text(
                        text = "有效敲击不足，请重试（需对准 click）",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "测得 ${formatOffset(measured)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "匹配 ${state.matchedTaps} 拍",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (measured != null) {
                        Button(
                            onClick = viewModel::applyMeasured,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text("写入补偿")
                        }
                    }
                    FilledTonalButton(
                        onClick = viewModel::startCalibration,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("重试")
                    }
                    OutlinedButton(onClick = viewModel::discardResult) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

private fun formatOffset(ms: Int): String =
    if (ms > 0) "+$ms ms" else "$ms ms"
