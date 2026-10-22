package com.tickbeat.app.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tickbeat.app.domain.Beatmap
import com.tickbeat.app.domain.HitGrade
import com.tickbeat.app.domain.SessionScore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Rhythm staff via offline VexFlow (WebView).
 * [playheadBeat] drives per-note highlight (no moving cursor).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VexRhythmView(
    beatmap: Beatmap,
    playheadBeat: Double? = null,
    score: SessionScore? = null,
    modifier: Modifier = Modifier
) {
    val pageVisible = LocalPageVisible.current
    val scoreJson = remember(beatmap) { beatmap.toVexScoreJson() }
    val beatsJson = remember(beatmap) {
        JSONArray(beatmap.events.map { it.tBeat }).toString()
    }
    val duration = beatmap.durationBeats.coerceAtLeast(1.0)
    val fg = MaterialTheme.colorScheme.onBackground.toArgb()
    val fgHex = String.format("#%06X", 0xFFFFFF and fg)
    val gradesJson = remember(score) {
        if (score == null) null
        else JSONArray(
            score.results.map {
                when (it.grade) {
                    HitGrade.Perfect -> "perfect"
                    HitGrade.Good -> "good"
                    HitGrade.Miss -> "miss"
                }
            }
        ).toString()
    }

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    // WebView CSS px ≈ dp; never pass physical pixels or the staff overflows.
    val fallbackWidthDp = (config.screenWidthDp - 40).coerceAtLeast(280)
    var measuredWidthDp by remember { mutableIntStateOf(0) }
    val staffWidthDp = if (measuredWidthDp > 0) measuredWidthDp else fallbackWidthDp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .onSizeChanged { size ->
                if (size.width > 0) {
                    val dp = with(density) { size.width.toDp().value.toInt() }
                    if (dp != measuredWidthDp) measuredWidthDp = dp
                }
            }
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            factory = { context ->
                val host = VexHost().apply {
                    pendingScore = scoreJson
                    pendingBeats = beatsJson
                    pendingDuration = duration
                    pendingFg = fgHex
                    pendingWidth = staffWidthDp
                    pendingGrades = gradesJson
                    pendingPlayhead = playheadBeat
                }
                FullWidthWebView(context).apply {
                    tag = host
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    isHorizontalScrollBarEnabled = false
                    isVerticalScrollBarEnabled = false
                    addJavascriptInterface(TickBeatJsBridge(), "TickBeat")
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            val h = view?.tag as? VexHost ?: return
                            h.ready = true
                            view.post { h.flush(view) }
                        }
                    }
                    loadUrl("file:///android_asset/vexflow/index.html")
                }
            },
            update = { webView ->
                // size(0.dp) keep-alive does not stop WebView from painting over other routes.
                webView.visibility = if (pageVisible) View.VISIBLE else View.GONE
                val host = webView.tag as? VexHost ?: return@AndroidView
                host.pendingScore = scoreJson
                host.pendingBeats = beatsJson
                host.pendingDuration = duration
                host.pendingFg = fgHex
                host.pendingWidth = staffWidthDp
                host.pendingGrades = gradesJson
                host.pendingPlayhead = playheadBeat
                if (pageVisible && host.ready) webView.post { host.flush(webView) }
            }
        )
    }
}

private class FullWidthWebView(context: Context) : WebView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            heightMeasureSpec
        )
    }
}

private class VexHost {
    var ready: Boolean = false
    private var lastScoreKey: String? = null

    var pendingScore: String? = null
    var pendingBeats: String? = null
    var pendingDuration: Double = 1.0
    var pendingFg: String = "#222222"
    var pendingWidth: Int = 0
    var pendingGrades: String? = null
    var pendingPlayhead: Double? = null

    fun flush(webView: WebView) {
        val score = pendingScore ?: return
        if (pendingWidth <= 0) return
        val beatsJson = pendingBeats ?: "[]"
        val gradesJson = pendingGrades
        val playheadBeat = pendingPlayhead
        val key = "$score|$pendingFg|$pendingWidth|${gradesJson.orEmpty()}"
        if (key != lastScoreKey) {
            lastScoreKey = key
            Log.d(TAG, "vex=\n$score")
            val js = buildString {
                append("renderScore(")
                append(JSONObject.quote(score))
                append(',')
                append(beatsJson)
                append(',')
                append(pendingDuration)
                append(',')
                append(JSONObject.quote(pendingFg))
                append(',')
                append(pendingWidth)
                append(',')
                append(pendingWidth)
                append(')')
            }
            webView.evaluateJavascript(js, null)
            if (gradesJson != null) {
                webView.evaluateJavascript("colorNotes($gradesJson)", null)
            }
        }
        if (playheadBeat != null) {
            webView.evaluateJavascript("setHighlight($playheadBeat)", null)
        } else {
            webView.evaluateJavascript("clearHighlight()", null)
        }
    }
}

private class TickBeatJsBridge {
    @JavascriptInterface
    fun onRendered(count: Int) {
        Log.d(TAG, "rendered notes=$count")
    }

    @JavascriptInterface
    fun onLayout(containerW: Int, engraverW: Int, svgW: Int) {
        Log.d(TAG, "layout container=$containerW engraver=$engraverW svg=$svgW")
    }
}

private const val TAG = "VexRhythm"
