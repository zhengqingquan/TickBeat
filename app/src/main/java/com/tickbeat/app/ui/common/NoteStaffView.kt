package com.tickbeat.app.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.Log
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
import com.tickbeat.app.domain.NoteClef
import com.tickbeat.app.domain.NoteReadFactory
import org.json.JSONObject

/**
 * Single pitched note on a treble/bass staff (offline VexFlow WebView).
 * [correct] null = neutral; true/false tints the note green/red after answer.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NoteStaffView(
    midi: Int,
    clef: NoteClef = NoteClef.Treble,
    correct: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val vexKey = remember(midi) { NoteReadFactory.vexKey(midi) }
    val clefName = clef.vexName
    val fg = MaterialTheme.colorScheme.onBackground.toArgb()
    val fgHex = String.format("#%06X", 0xFFFFFF and fg)
    val grade = when (correct) {
        true -> "correct"
        false -> "wrong"
        null -> null
    }

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val fallbackWidthDp = (config.screenWidthDp - 40).coerceAtLeast(280)
    var measuredWidthDp by remember { mutableIntStateOf(0) }
    val staffWidthDp = if (measuredWidthDp > 0) measuredWidthDp else fallbackWidthDp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
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
                .height(180.dp),
            factory = { context ->
                val host = NoteStaffHost().apply {
                    pendingKey = vexKey
                    pendingClef = clefName
                    pendingFg = fgHex
                    pendingWidth = staffWidthDp
                    pendingGrade = grade
                }
                NoteStaffWebView(context).apply {
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
                    addJavascriptInterface(NoteStaffJsBridge(), "TickBeat")
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            val h = view?.tag as? NoteStaffHost ?: return
                            h.ready = true
                            view.post { h.flush(view) }
                        }
                    }
                    loadUrl("file:///android_asset/vexflow/note.html")
                }
            },
            update = { webView ->
                val host = webView.tag as? NoteStaffHost ?: return@AndroidView
                host.pendingKey = vexKey
                host.pendingClef = clefName
                host.pendingFg = fgHex
                host.pendingWidth = staffWidthDp
                host.pendingGrade = grade
                if (host.ready) webView.post { host.flush(webView) }
            }
        )
    }
}

private class NoteStaffWebView(context: Context) : WebView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            heightMeasureSpec
        )
    }
}

private class NoteStaffHost {
    var ready: Boolean = false
    private var lastRender: String? = null
    private var lastGrade: String? = ""

    var pendingKey: String = "c/4"
    var pendingClef: String = "treble"
    var pendingFg: String = "#222222"
    var pendingWidth: Int = 0
    var pendingGrade: String? = null

    fun flush(webView: WebView) {
        if (pendingWidth <= 0) return
        val key = pendingKey
        val clef = pendingClef
        val grade = pendingGrade
        val renderKey = "$key|$clef|$pendingFg|$pendingWidth"
        if (renderKey != lastRender) {
            lastRender = renderKey
            lastGrade = grade
            val js = buildString {
                append("renderNote(")
                append(JSONObject.quote(key))
                append(',')
                append(JSONObject.quote(pendingFg))
                append(',')
                append(pendingWidth)
                append(',')
                if (grade == null) append("null") else append(JSONObject.quote(grade))
                append(',')
                append(JSONObject.quote(clef))
                append(')')
            }
            webView.evaluateJavascript(js, null)
            return
        }
        if (grade != lastGrade) {
            lastGrade = grade
            if (grade == null) {
                webView.evaluateJavascript("setGrade(null)", null)
            } else {
                webView.evaluateJavascript("setGrade(${JSONObject.quote(grade)})", null)
            }
        }
    }
}

private class NoteStaffJsBridge {
    @JavascriptInterface
    fun onNoteRendered(key: String) {
        Log.d(TAG, "note rendered key=$key")
    }
}

private const val TAG = "NoteStaff"
