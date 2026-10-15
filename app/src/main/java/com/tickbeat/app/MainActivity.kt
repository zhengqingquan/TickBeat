package com.tickbeat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tickbeat.app.ui.TickBeatApp
import com.tickbeat.app.ui.theme.TickBeatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TickBeatTheme {
                TickBeatApp()
            }
        }
    }
}
