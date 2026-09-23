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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private object MermaidRenderLimiter {
    val semaphore = Semaphore(2)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidWebViewPresenter(
    diagramCode: String,
    themeMode: String, // "light", "dark", "sepia"
    modifier: Modifier = Modifier
) {
    var webViewHeight by remember { mutableStateOf(180.dp) }
    var renderError by remember { mutableStateOf<String?>(null) }
    var isExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }
    var renderedKey by remember { mutableStateOf("") }

    // Guards against state writes and WebView usage after the view has been released/destroyed.
    var isReleased by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun triggerRender(view: WebView?) {
        if (view == null || !isPageLoaded || isReleased) return
        val key = "$diagramCode|$themeMode"
        if (key == renderedKey && !isLoading) return
        isLoading = true
        val base64Code = Base64.encodeToString(diagramCode.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        view.evaluateJavascript("renderMermaidBase64('$base64Code', '$themeMode');", null)
    }

    // Rate-limited render execution using the shared semaphore
    LaunchedEffect(isPageLoaded, diagramCode, themeMode, webViewRef) {
        if (isPageLoaded && webViewRef != null && !isReleased) {
            val key = "$diagramCode|$themeMode"
            if (key != renderedKey) {
                MermaidRenderLimiter.semaphore.withPermit {
                    triggerRender(webViewRef)
                }
            }
        }
    }

    // 8-second render timeout — resets whenever diagramCode or themeMode changes
    LaunchedEffect(diagramCode, themeMode) {
        isLoading = true
        renderError = null
        delay(8_000L)
        if (isLoading && renderedKey != "$diagramCode|$themeMode") {
            renderError = "Render timeout — diagram source shown below"
            isLoading = false
        }
    }

    if (renderError != null) {
        // Compact "Diagram unavailable" card with expandable source section
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Diagram unavailable",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Hide Source" else "View Source",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            text = "Reason: ${renderError ?: "Malformed diagram code"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = diagramCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // wrapContentHeight() ensures the Box never collapses to zero before height is resolved
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(webViewHeight),
                factory = { context ->
                    WebView(context).apply {
                        webViewRef = this
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.blockNetworkLoads = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.domStorageEnabled = false
                        settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (url?.startsWith("file:///android_asset/mermaid/mermaid.min.html") == true) {
                                    isPageLoaded = true
                                    mainHandler.post {
                                        triggerRender(view)
                                    }
                                }
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                android.util.Log.e("InkleafCrash", "Mermaid WebView render process terminated: didCrash=${detail?.didCrash()}")
                                if (isReleased) return true
                                mainHandler.post {
                                    if (isReleased) return@post
                                    renderError = "WebView render process terminated (memory pressure)"
                                    isLoading = false
                                }
                                return true
                            }
                        }

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onPageReady() {
                                if (isReleased) return
                                mainHandler.post {
                                    if (isReleased) return@post
                                    isPageLoaded = true
                                    triggerRender(this@apply)
                                }
                            }

                            @JavascriptInterface
                            fun onRenderSuccess(width: Int, height: Int) {
                                if (isReleased) return
                                mainHandler.post {
                                    if (isReleased) return@post
                                    renderedKey = "$diagramCode|$themeMode"
                                    val density = resources.displayMetrics.density
                                    val computedHeightDp = (height / density).coerceAtLeast(80f).coerceAtMost(2500f)
                                    webViewHeight = computedHeightDp.dp
                                    isLoading = false
                                    renderError = null
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

                        loadUrl("file:///android_asset/mermaid/mermaid.min.html")
                    }
                },
                update = { webView ->
                    if (isReleased) return@AndroidView
                    webViewRef = webView
                    val key = "$diagramCode|$themeMode"
                    if (isPageLoaded && key != renderedKey) {
                        triggerRender(webView)
                    }
                },
                onRelease = { webView ->
                    // Detach the bridge and stop loading BEFORE destroy() so no late JS
                    // callback touches a destroyed WebView (native crash guard).
                    isReleased = true
                    webViewRef = null
                    mainHandler.post {
                        webView.stopLoading()
                        webView.removeJavascriptInterface("AndroidBridge")
                        webView.destroy()
                    }
                }
            )

            // Skeleton progress bar — hidden once render succeeds or errors out
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
