package com.inkleaf.app.ui.reader

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.webkit.WebViewAssetLoader
import com.inkleaf.app.BuildConfig
import com.inkleaf.app.domain.model.MermaidBlock
import com.inkleaf.app.ui.theme.ReaderThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun detectDiagramType(code: String): String {
    val cleanLine = code.lines()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() && !it.startsWith("%%") } ?: ""

    val lower = cleanLine.lowercase()
    return when {
        lower.startsWith("flowchart") || lower.startsWith("graph") -> "Flowchart"
        lower.startsWith("sequencediagram") -> "Sequence Diagram"
        lower.startsWith("classdiagram") -> "Class Diagram"
        lower.startsWith("statediagram") -> "State Diagram"
        lower.startsWith("erdiagram") -> "ER Diagram"
        lower.startsWith("gantt") -> "Gantt Chart"
        lower.startsWith("pie") -> "Pie Chart"
        lower.startsWith("gitgraph") -> "Git Graph"
        lower.startsWith("journey") -> "User Journey"
        lower.startsWith("mindmap") -> "Mindmap"
        lower.startsWith("quadrantchart") -> "Quadrant Chart"
        lower.startsWith("timeline") -> "Timeline"
        lower.startsWith("sankey") -> "Sankey Diagram"
        lower.startsWith("zenuml") -> "ZenUML"
        else -> "Mermaid Diagram"
    }
}

@Composable
fun MermaidBlockPresenter(
    block: MermaidBlock,
    themeMode: ReaderThemeMode,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val diagramType = remember(block.diagramCode) { detectDiagramType(block.diagramCode) }
    val lineCount = remember(block.diagramCode) { block.diagramCode.lines().size }
    val codePreview = remember(block.diagramCode) {
        val nonCommentLines = block.diagramCode.lines()
            .filter { it.isNotBlank() && !it.trim().startsWith("%%") }
        nonCommentLines.take(3).joinToString("\n")
    }

    val isDark = themeMode == ReaderThemeMode.DARK
    val isSepia = themeMode == ReaderThemeMode.SEPIA

    val cardBg = when {
        isDark -> Color(0xFF0F172A)
        isSepia -> Color(0xFFF3ECE0)
        else -> Color(0xFFF8FAFC)
    }

    val headerBg = when {
        isDark -> Color(0xFF1E293B)
        isSepia -> Color(0xFFE8DFC8)
        else -> Color(0xFFEDF2F7)
    }

    val borderColor = when {
        isDark -> Color(0xFF334155)
        isSepia -> Color(0xFFD4C5A9)
        else -> Color(0xFFE2E8F0)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .heightIn(min = 42.dp)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                    isDark -> Color(0xFF3B82F6)
                    isSepia -> Color(0xFF8D3E1B)
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = "MERMAID",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = diagramType,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isDark -> Color(0xFFCBD5E1)
                    isSepia -> Color(0xFF573A25)
                    else -> Color(0xFF475569)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "$lineCount ${if (lineCount == 1) "line" else "lines"}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = when {
                    isDark -> Color(0xFF64748B)
                    isSepia -> Color(0xFF8C7355)
                    else -> Color(0xFF94A3B8)
                }
            )
        }

        HorizontalDivider(color = borderColor)

        // Code preview snippet
        if (codePreview.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = codePreview,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = when {
                        isDark -> Color(0xFF94A3B8)
                        isSepia -> Color(0xFF6E5339)
                        else -> Color(0xFF64748B)
                    },
                    maxLines = 3
                )
            }
        }

        // Action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isDark -> Color(0xFF2563EB)
                        isSepia -> Color(0xFF8D3E1B)
                        else -> MaterialTheme.colorScheme.primary
                    }
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "📊 View Diagram",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(block.diagramCode))
                    isCopied = true
                    coroutineScope.launch {
                        delay(2000)
                        isCopied = false
                    }
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isCopied) Color(0xFF10B981) else when {
                        isDark -> Color(0xFF94A3B8)
                        isSepia -> Color(0xFF6E5339)
                        else -> Color(0xFF475569)
                    }
                )
            ) {
                Text(
                    text = if (isCopied) "✓ Copied" else "Copy Source",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showDialog) {
        MermaidViewerDialog(
            diagramCode = block.diagramCode,
            themeMode = themeMode,
            onDismiss = { showDialog = false }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidViewerDialog(
    diagramCode: String,
    themeMode: ReaderThemeMode,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    var isReleased by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val diagramType = remember(diagramCode) { detectDiagramType(diagramCode) }

    val isDark = themeMode == ReaderThemeMode.DARK
    val isSepia = themeMode == ReaderThemeMode.SEPIA

    val dialogBg = when {
        isDark -> Color(0xFF0F172A)
        isSepia -> Color(0xFFFBF0D9)
        else -> Color(0xFFFFFFFF)
    }

    val headerBg = when {
        isDark -> Color(0xFF1E293B)
        isSepia -> Color(0xFFF2E5CA)
        else -> Color(0xFFF1F5F9)
    }

    val headerBorder = when {
        isDark -> Color(0xFF334155)
        isSepia -> Color(0xFFE2D3B3)
        else -> Color(0xFFE2E8F0)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, headerBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = dialogBg,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 $diagramType",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isDark -> Color(0xFFF8FAFC)
                            isSepia -> Color(0xFF433422)
                            else -> Color(0xFF0F172A)
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(diagramCode))
                            isCopied = true
                            coroutineScope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isCopied) Color(0xFF10B981) else when {
                                isDark -> Color(0xFF93C5FD)
                                isSepia -> Color(0xFF8D3E1B)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Text(
                            text = if (isCopied) "✓ Copied" else "Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Diagram",
                            tint = when {
                                isDark -> Color(0xFF94A3B8)
                                isSepia -> Color(0xFF6E5339)
                                else -> Color(0xFF64748B)
                            }
                        )
                    }
                }

                HorizontalDivider(color = headerBorder)

                // Offline Hardware-Accelerated WebView Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(dialogBg)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            if (com.inkleaf.app.BuildConfig.DEBUG) {
                                WebView.setWebContentsDebuggingEnabled(true)
                            }
                            WebView(context).apply {
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                setBackgroundColor(AndroidColor.TRANSPARENT)

                                val assetLoader = WebViewAssetLoader.Builder()
                                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                                    .build()

                                settings.apply {
                                    javaScriptEnabled = true
                                    blockNetworkLoads = true
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    domStorageEnabled = false
                                    cacheMode = WebSettings.LOAD_NO_CACHE
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportZoom(true)
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(
                                        view: WebView,
                                        request: WebResourceRequest
                                    ): WebResourceResponse? {
                                        return assetLoader.shouldInterceptRequest(request.url)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (isReleased) return

                                        val base64Code = android.util.Base64.encodeToString(
                                            diagramCode.toByteArray(Charsets.UTF_8),
                                            android.util.Base64.NO_WRAP
                                        )
                                        val themeParam = when (themeMode) {
                                            ReaderThemeMode.DARK -> "dark"
                                            ReaderThemeMode.SEPIA -> "sepia"
                                            else -> "light"
                                        }

                                        view?.evaluateJavascript(
                                            "renderFromBase64('$base64Code', '$themeParam');",
                                            null
                                        )
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        android.util.Log.e("InkleafMermaid", "Mermaid render process terminated: didCrash=${detail?.didCrash()}")
                                        return true
                                    }
                                }

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onRenderSuccess() {
                                        // Bridge hook for host notification
                                    }

                                    @JavascriptInterface
                                    fun onRenderError(error: String) {
                                        android.util.Log.w("InkleafMermaid", "Mermaid syntax error: $error")
                                    }
                                }, "AndroidBridge")

                                loadUrl("https://appassets.androidplatform.net/assets/mermaid/template.html")
                            }
                        },
                        update = { webView ->
                            if (isReleased) return@AndroidView
                            val base64Code = android.util.Base64.encodeToString(
                                diagramCode.toByteArray(Charsets.UTF_8),
                                android.util.Base64.NO_WRAP
                            )
                            val themeParam = when (themeMode) {
                                ReaderThemeMode.DARK -> "dark"
                                ReaderThemeMode.SEPIA -> "sepia"
                                else -> "light"
                            }
                            webView.evaluateJavascript(
                                "renderFromBase64('$base64Code', '$themeParam');",
                                null
                            )
                        },
                        onRelease = { webView ->
                            isReleased = true
                            mainHandler.post {
                                webView.stopLoading()
                                webView.removeJavascriptInterface("AndroidBridge")
                                webView.destroy()
                            }
                        }
                    )
                }
            }
        }
    }
}
