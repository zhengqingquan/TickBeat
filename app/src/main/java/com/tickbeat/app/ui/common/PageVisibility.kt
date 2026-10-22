package com.tickbeat.app.ui.common

import androidx.compose.runtime.staticCompositionLocalOf

/** Whether the current keep-alive page is the visible route. WebViews must honor this. */
val LocalPageVisible = staticCompositionLocalOf { true }
