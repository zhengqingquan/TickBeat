package com.tickbeat.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tickbeat.app.domain.IntervalQuestionFactory
import com.tickbeat.app.domain.IntervalRange

private data class PianoKey(
    val midi: Int,
    val isBlack: Boolean,
    /** White-key index 0..7; black keys sit after this white index. */
    val whiteIndex: Int
)

/** One octave inclusive of both ends (C..C): 8 white keys, 5 black keys. */
private fun buildOctaveKeys(lowMidi: Int): List<PianoKey> {
    val whiteOffsets = listOf(0, 2, 4, 5, 7, 9, 11, 12)
    val blackOffsets = listOf(1, 3, 6, 8, 10)
    val blackAfterWhite = listOf(0, 1, 3, 4, 5)
    val whites = whiteOffsets.mapIndexed { i, off ->
        PianoKey(midi = lowMidi + off, isBlack = false, whiteIndex = i)
    }
    val blacks = blackOffsets.mapIndexed { i, off ->
        PianoKey(midi = lowMidi + off, isBlack = true, whiteIndex = blackAfterWhite[i])
    }
    return whites + blacks
}

@Composable
fun PianoKeyboard(
    onNoteDown: (midi: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlightMidi: Int? = null,
    lowMidi: Int = IntervalRange.LOW_MIDI,
    highMidi: Int = IntervalRange.HIGH_MIDI
) {
    val keys = remember(lowMidi) { buildOctaveKeys(lowMidi) }
    val whiteKeys = remember(keys) { keys.filter { !it.isBlack } }
    val blackKeys = remember(keys) { keys.filter { it.isBlack } }
    var pressedMidi by remember { mutableStateOf<Int?>(null) }

    val whiteFill = MaterialTheme.colorScheme.surface
    val whitePressed = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
    val whiteHighlight = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
    val blackFill = Color(0xFF1A1A1A)
    val blackPressed = MaterialTheme.colorScheme.secondary
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
    ) {
        val whiteWidth = maxWidth / whiteKeys.size
        val blackWidth = whiteWidth * 0.62f

        Row(modifier = Modifier.fillMaxSize()) {
            whiteKeys.forEach { key ->
                val bg = when {
                    pressedMidi == key.midi -> whitePressed
                    highlightMidi == key.midi -> whiteHighlight
                    else -> whiteFill
                }
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .width(whiteWidth)
                        .fillMaxHeight()
                        .border(1.dp, borderColor)
                        .background(bg)
                        .notePress(enabled, key.midi, onNoteDown) { pressedMidi = it }
                ) {
                    if (key.midi == lowMidi || key.midi == highMidi) {
                        Text(
                            text = IntervalQuestionFactory.midiName(key.midi),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        blackKeys.forEach { key ->
            val left = whiteWidth * (key.whiteIndex + 1) - blackWidth / 2
            val bg = if (pressedMidi == key.midi || highlightMidi == key.midi) {
                blackPressed
            } else {
                blackFill
            }
            Box(
                modifier = Modifier
                    .zIndex(1f)
                    .offset(x = left)
                    .width(blackWidth)
                    .fillMaxHeight(0.58f)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(bg)
                    .border(
                        width = 1.dp,
                        color = Color.Black.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                    )
                    .notePress(enabled, key.midi, onNoteDown) { pressedMidi = it }
            )
        }
    }
}

private fun Modifier.notePress(
    enabled: Boolean,
    midi: Int,
    onNoteDown: (Int) -> Unit,
    setPressed: (Int?) -> Unit
): Modifier = pointerInput(enabled, midi) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        setPressed(midi)
        onNoteDown(midi)
        do {
            val event = awaitPointerEvent()
            if (event.changes.none { it.pressed }) break
        } while (true)
        setPressed(null)
    }
}
