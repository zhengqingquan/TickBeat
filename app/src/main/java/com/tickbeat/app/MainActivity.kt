package com.tickbeat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickbeat.app.metronome.MetronomeViewModel
import com.tickbeat.app.ui.MetronomeScreen
import com.tickbeat.app.ui.theme.TickBeatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TickBeatTheme {
                // Avoid collecting beat-heavy state here — keep Activity composition stable.
                MetronomeScreen(viewModel = viewModel())
            }
        }
    }
}
