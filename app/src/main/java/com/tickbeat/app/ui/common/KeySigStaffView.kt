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
import com.tickbeat.app.domain.NoteClef
import org.json.JSONObject

/** Empty staff with clef + key signature (offline VexFlow WebView). */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KeySigStaffView(
    vexKey: String,
    clef: NoteClef = NoteClef.Treble,
    modifier: Modifier = Modifier
) {
    val pageVisible = LocalPageVisible.current
    val clefName = clef.vexName
    val fg = MaterialTheme.colorScheme.onBackground.toArgb()
    val fgHex = String.format("#%06X", 0xFFFFFF and fg)

    val density = LocalDensity.current
    val config = LocalConfiguration.current
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
                val host = KeySigHost().apply {
                    pendingKeySig = vexKey
                    pendingClef = clefName
                    pendingFg = fgHex
                    pendingWidth = staffWidthDp
                }
                KeySigWebView(context).apply {
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
                    addJavascriptInterface(KeySigJsBridge(), "TickBeat")
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            val h = view?.tag as? KeySigHost ?: return
                            h.ready = true
                            view.post { h.flush(view) }
                        }
                    }
                    loadUrl("file:///android_asset/vexflow/keysig.html")
                }
            },
            update = { webView ->
                webView.visibility = if (pageVisible) View.VISIBLE else View.GONE
                val host = webView.tag as? KeySigHost ?: return@AndroidView
                host.pendingKeySig = vexKey
                host.pendingClef = clefName
                host.pendingFg = fgHex
                host.pendingWidth = staffWidthDp
                if (pageVisible && host.ready) webView.post { host.flush(webView) }
            }
        )
    }
}

private class KeySigWebView(context: Context) : WebView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            heightMeasureSpec
        )
    }
}

private class KeySigHost {
    var ready: Boolean = false
    private var lastRender: String? = null

    var pendingKeySig: String = "C"
    var pendingClef: String = "treble"
    var pendingFg: String = "#222222"
    var pendingWidth: Int = 0

    fun flush(webView: WebView) {
        if (pendingWidth <= 0) return
        val renderKey = "$pendingKeySig|$pendingClef|$pendingFg|$pendingWidth"
        if (renderKey == lastRender) return
        lastRender = renderKey
        val js = buildString {
            append("renderKeySig(")
            append(JSONObject.quote(pendingKeySig))
            append(',')
            append(JSONObject.quote(pendingFg))
            append(',')
            append(pendingWidth)
            append(',')
            append(JSONObject.quote(pendingClef))
            append(')')
        }
        webView.evaluateJavascript(js, null)
    }
}

private class KeySigJsBridge {
    @JavascriptInterface
    fun onKeySigRendered(keySig: String, clef: String) {
        Log.d(TAG, "keySig rendered key=$keySig clef=$clef")
    }
}

private const val TAG = "KeySigStaff"
