package com.tickbeat.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tickbeat.app.domain.SessionScore

@Composable
fun ScoreReportCard(
    score: SessionScore,
    onRetry: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "练习结果",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text("得分 ${score.score}")
            Text("准确度 ${score.accuracyPercent}%")
            Text("Perfect ${score.perfectCount} · Good ${score.goodCount} · Miss ${score.missCount}")
            Text("平均误差 ${"%.0f".format(score.averageAbsErrorMs)} ms")
            Text("最大连击 ${score.maxCombo}")
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row {
                if (onRetry != null) {
                    TextButton(onClick = onRetry) { Text("再来一次") }
                }
                if (onDone != null) {
                    TextButton(onClick = onDone) { Text("完成") }
                }
            }
        }
    }
}
