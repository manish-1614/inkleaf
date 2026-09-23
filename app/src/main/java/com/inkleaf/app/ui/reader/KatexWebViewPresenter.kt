package com.inkleaf.app.ui.reader

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KatexWebViewPresenter(
    latexFormula: String,
    isInline: Boolean,
    modifier: Modifier = Modifier
) {
    var webViewHeight by remember { mutableStateOf(50.dp) }
    var renderError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Guards against state writes and WebView usage after the view has been released/destroyed.
    var isReleased by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var lastRenderedKey by remember { mutableStateOf("") }

    // 5-second render timeout — resets if latexFormula changes
    LaunchedEffect(latexFormula, isInline) {
        isLoading = true
        renderError = null
        delay(5_000L)
        if (isLoading) {
            renderError = "Math render timeout — formula shown as text"
            isLoading = false
        }
    }

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
                .height(webViewHeight),
            contentAlignment = Alignment.TopCenter
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(webViewHeight),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.blockNetworkLoads = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.domStorageEnabled = false
                        settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (url?.startsWith("file:///android_asset/katex/katex.min.html") == true) {
                                    val base64Formula = Base64.encodeToString(latexFormula.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                                    view?.evaluateJavascript(
                                        "renderMathBase64('$base64Formula', $isInline);",
                                        null
                                    )
                                }
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                android.util.Log.e("InkleafCrash", "KaTeX WebView render process terminated: didCrash=${detail?.didCrash()}")
                                if (isReleased) return true
                                mainHandler.post {
                                    if (isReleased) return@post
                                    renderError = "Math WebView render process terminated"
                                    isLoading = false
                                }
                                return true
                            }
                        }

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onRenderSuccess(width: Int, height: Int) {
                                if (isReleased) return
                                mainHandler.post {
                                    if (isReleased) return@post
                                    lastRenderedKey = "$latexFormula|$isInline"
                                    val density = resources.displayMetrics.density
                                    val computedHeightDp = (height / density).coerceAtLeast(36f).coerceAtMost(800f)
                                    webViewHeight = computedHeightDp.dp
                                    isLoading = false
                                }
                            }

                            @JavascriptInterface
                            fun onRenderError(error: String) {
                                if (isReleased) return
                                mainHandler.post {
                                    if (isReleased) return@post
                                    renderError = error
                                    isLoading = false
                                }
                            }
                        }, "AndroidBridge")

                        loadUrl("file:///android_asset/katex/katex.min.html")
                    }
                },
                update = { webView ->
                    if (isReleased) return@AndroidView
                    val key = "$latexFormula|$isInline"
                    if (key != lastRenderedKey && lastRenderedKey.isNotEmpty()) {
                        lastRenderedKey = key
                        isLoading = true
                        val base64Formula = Base64.encodeToString(latexFormula.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                        webView.evaluateJavascript(
                            "renderMathBase64('$base64Formula', $isInline);",
                            null
                        )
                    }
                },
                onRelease = { webView ->
                    // Detach the bridge and stop loading BEFORE destroy() so no late JS
                    // callback touches a destroyed WebView (native crash guard).
                    isReleased = true
                    mainHandler.post {
                        webView.stopLoading()
                        webView.removeJavascriptInterface("AndroidBridge")
                        webView.destroy()
                    }
                }
            )

            // Loading indicator — hidden once KaTeX render succeeds or errors out
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

