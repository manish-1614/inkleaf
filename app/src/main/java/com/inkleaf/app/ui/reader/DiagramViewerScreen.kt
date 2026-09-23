package com.inkleaf.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkleaf.app.data.diagram.DiagramRepository
import com.inkleaf.app.domain.plugin.MermaidRenderPlugin
import com.inkleaf.app.ui.theme.ReaderThemeMode
import com.inkleaf.app.ui.theme.ReaderThemePalette
import com.inkleaf.app.ui.theme.resolvePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagramViewerScreen(
    diagramId: String,
    themeMode: ReaderThemeMode,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val diagramSource = remember(diagramId) {
        DiagramRepository.getInstance().getDiagram(diagramId)
    }

    val isDark = isSystemInDarkTheme()
    val palette = themeMode.resolvePalette(isDark)
    val resolvedThemeMode = when (palette) {
        ReaderThemePalette.DARK -> "dark"
        ReaderThemePalette.PAPER -> "paper"
        ReaderThemePalette.LIGHT -> "light"
    }

    val previewTitle = remember(diagramSource) {
        diagramSource?.let { MermaidRenderPlugin().extractPreviewLabel(it) } ?: "Diagram Viewer"
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    fun resetTransform() {
        scale = 1f
        offset = Offset.Zero
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = previewTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val zoomPercent = (scale * 100).toInt()
                        Text(
                            text = if (scale != 1f || offset != Offset.Zero) "Zoom: $zoomPercent% • Drag to pan" else "Pinch to zoom • Double-tap to reset",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to document"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            resetTransform()
                            reloadTrigger++
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload diagram"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (diagramSource == null) {
                // Empty / error state: diagramId not found in cache
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Diagram Not Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "The requested diagram could not be retrieved from the active document cache (ID: $diagramId).\nIt may have expired or the document was reloaded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Back to Document")
                        }
                    }
                }
            } else {
                // Full-bleed zoomable & pannable container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(diagramId) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale != 1f) {
                                        resetTransform()
                                    } else {
                                        scale = 2.2f
                                    }
                                }
                            )
                        }
                        .pointerInput(diagramId) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 6.0f)
                                val maxBound = 3000f * scale
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxBound, maxBound),
                                    y = (offset.y + pan.y).coerceIn(-maxBound, maxBound)
                                )
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        key(diagramId, reloadTrigger) {
                            MermaidWebViewPresenter(
                                diagramCode = diagramSource,
                                themeMode = resolvedThemeMode,
                                modifier = Modifier.fillMaxSize(),
                                isFullScreen = true
                            )
                        }
                    }
                }

                // Floating indicator when zoomed
                AnimatedVisibility(
                    visible = scale != 1f || offset != Offset.Zero,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .navigationBarsPadding()
                ) {
                    Surface(
                        onClick = { resetTransform() },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fit to Screen",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
