package com.inkleaf.app.ui.reader

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KatexWebViewPresenter(
    latexFormula: String,
    isInline: Boolean,
    modifier: Modifier = Modifier
) {
    var webViewHeight by remember { mutableStateOf(50.dp) }
    var renderError by remember { mutableStateOf<String?>(null) }

    if (renderError != null) {
        // Safe graceful text fallback on parsing error
        Text(
            text = "Failed to render math: $renderError\nFormula: $latexFormula",
            modifier = modifier.fillMaxWidth()
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .height(webViewHeight)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.blockNetworkLoads = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val escapedFormula = latexFormula.replace("`", "\\`").replace("\\", "\\\\")
                                view?.evaluateJavascript(
                                    "renderMath(`$escapedFormula`, $isInline);",
                                    null
                                )
                            }
                        }

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onRenderSuccess(width: Int, height: Int) {
                                val density = resources.displayMetrics.density
                                val computedHeightDp = (height / density).coerceAtLeast(40f).coerceAtMost(250f)
                                webViewHeight = computedHeightDp.dp
                            }

                            @JavascriptInterface
                            fun onRenderError(error: String) {
                                renderError = error
                            }
                        }, "AndroidBridge")

                        loadUrl("file:///android_asset/katex/katex.min.html")
                    }
                },
                update = { webView ->
                    // Handle update
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.destroy()
                }
            )
        }
    }
}
