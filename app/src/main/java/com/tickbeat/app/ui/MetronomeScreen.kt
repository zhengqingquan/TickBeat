package com.tickbeat.app.ui

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import android.widget.EditText
import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickbeat.app.metronome.MetronomeViewModel
import com.tickbeat.app.metronome.TimeSignature
import com.tickbeat.app.ui.theme.TickBeatBeatActive
import com.tickbeat.app.ui.theme.TickBeatBeatIdle

@Composable
fun MetronomeScreen(
    viewModel: MetronomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    // Inflate NumberPickers after first frames so the main chrome paints immediately.
    var wheelsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withFrameMillis { }
        withFrameMillis { }
        wheelsReady = true
    }

    DisposableEffect(state.isPlaying, activity) {
        val window = activity?.window
        if (window != null) {
            if (state.isPlaying) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Parent already applies TopAppBar insets; avoid stacking extra top space.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::togglePlaying,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "停止" else "开始"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BeatIndicators(
                viewModel = viewModel,
                beatsPerBar = state.timeSignature.beats,
                isPlaying = state.isPlaying
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "${state.bpm}",
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-2).sp
            )
            Text(
                text = "BPM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { viewModel.adjustBpm(-1) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "减慢")
                }
                Spacer(Modifier.width(28.dp))
                IconButton(
                    onClick = { viewModel.adjustBpm(1) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "加快")
                }
            }

            Spacer(Modifier.height(4.dp))

            Slider(
                value = state.bpm.toFloat(),
                onValueChange = { viewModel.setBpm(it.toInt()) },
                valueRange = 30f..300f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "拍号  ${state.timeSignature.label}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))

            TimeSignaturePresetChips(
                selected = state.timeSignature,
                onSelect = viewModel::setTimeSignature
            )

            Spacer(Modifier.height(12.dp))

            if (wheelsReady) {
                TimeSignatureWheelPicker(
                    beats = state.timeSignature.beats,
                    noteValue = state.timeSignature.noteValue,
                    onBeatsChange = viewModel::setBeats,
                    onNoteValueChange = viewModel::setNoteValue
                )
            } else {
                Spacer(Modifier.height(156.dp))
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeSignaturePresetChips(
    selected: TimeSignature,
    onSelect: (TimeSignature) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TimeSignature.Presets.forEach { signature ->
            FilterChip(
                selected = selected == signature,
                onClick = { onSelect(signature) },
                label = { Text(signature.label) }
            )
        }
    }
}

@Composable
private fun TimeSignatureWheelPicker(
    beats: Int,
    noteValue: Int,
    onBeatsChange: (Int) -> Unit,
    onNoteValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        NumberWheel(
            values = TimeSignature.BeatOptions,
            selected = beats,
            onSelected = onBeatsChange,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "/",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        NumberWheel(
            values = TimeSignature.NoteValueOptions,
            selected = noteValue,
            onSelected = onNoteValueChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NumberWheel(
    values: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val onSelectedState = rememberUpdatedOnSelected(onSelected)
    val valuesKey = remember(values) { values.toList() }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val dividerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f).toArgb()
    val dividerHeightPx = with(LocalDensity.current) { 1.5.dp.roundToPx() }

    AndroidView(
        modifier = modifier.height(140.dp),
        factory = { context ->
            NumberPicker(context).apply {
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                wrapSelectorWheel = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                minValue = 0
                maxValue = (valuesKey.size - 1).coerceAtLeast(0)
                displayedValues = valuesKey.map { it.toString() }.toTypedArray()
                value = valuesKey.indexOf(selected).coerceAtLeast(0)
                applyTickBeatNumberPickerStyle(textColor, dividerColor, dividerHeightPx)
                setOnValueChangedListener { _, _, newIndex ->
                    if (newIndex in valuesKey.indices) {
                        onSelectedState(valuesKey[newIndex])
                    }
                }
            }
        },
        update = { picker ->
            picker.applyTickBeatNumberPickerStyle(textColor, dividerColor, dividerHeightPx)
            val display = valuesKey.map { it.toString() }.toTypedArray()
            val lastIndex = (valuesKey.size - 1).coerceAtLeast(0)
            if (picker.maxValue != lastIndex ||
                picker.displayedValues?.contentEquals(display) != true
            ) {
                picker.displayedValues = null
                picker.minValue = 0
                picker.maxValue = lastIndex
                picker.displayedValues = display
            }
            val index = valuesKey.indexOf(selected).coerceAtLeast(0)
            if (picker.value != index) {
                picker.value = index
            }
        }
    )
}

private fun NumberPicker.applyTickBeatNumberPickerStyle(
    textColor: Int,
    dividerColor: Int,
    dividerHeightPx: Int
) {
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.textColor = textColor
        selectionDividerHeight = dividerHeightPx
    } else {
        for (i in 0 until childCount) {
            (getChildAt(i) as? EditText)?.setTextColor(textColor)
        }
    }
    // Soft navy selection rails; ColorDrawable replaces the stock gray/blue divider.
    try {
        val field = NumberPicker::class.java.getDeclaredField("mSelectionDivider")
        field.isAccessible = true
        field.set(this, ColorDrawable(dividerColor))
    } catch (_: Exception) {
        // Field removed on some OEM/API builds; text/bg styling still applies.
    }
}

@Composable
private fun rememberUpdatedOnSelected(onSelected: (Int) -> Unit): (Int) -> Unit {
    val latest = remember { mutableStateOf(onSelected) }
    latest.value = onSelected
    return remember {
        { value: Int -> latest.value(value) }
    }
}

@Composable
private fun BeatIndicators(
    viewModel: MetronomeViewModel,
    beatsPerBar: Int,
    isPlaying: Boolean
) {
    val currentBeat by viewModel.currentBeat.collectAsStateWithLifecycle()

    // Fixed outer height so changing beat count doesn't shift BPM / controls vertically.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            repeat(beatsPerBar) { index ->
                val beatNumber = index + 1
                val active = isPlaying && currentBeat == beatNumber
                Box(
                    modifier = Modifier
                        .size(if (index == 0) 20.dp else 16.dp)
                        .clip(CircleShape)
                        .background(if (active) TickBeatBeatActive else TickBeatBeatIdle)
                )
            }
        }
    }
}
