package com.inkleaf.app.ui.reader

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkleaf.app.data.preferences.ReaderPreferencesRepository
import com.inkleaf.app.data.saf.DocumentMetadata
import com.inkleaf.app.data.saf.SafDocumentRepository
import com.inkleaf.app.domain.model.*
import com.inkleaf.app.domain.parser.MarkdownBlockParser
import com.inkleaf.app.domain.parser.ParsedDocument
import com.inkleaf.app.ui.theme.ReaderThemeMode
import com.inkleaf.app.ui.theme.displayLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    documentUri: Uri,
    safRepository: SafDocumentRepository,
    preferencesRepository: ReaderPreferencesRepository,
    themeMode: ReaderThemeMode,
    onBack: () -> Unit,
    onThemeChange: ((ReaderThemeMode) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf<UiState<Pair<DocumentMetadata?, ParsedDocument>>>(UiState.Loading) }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var currentMatchIndex by remember { mutableStateOf(0) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var isNavigatingByToc by remember { mutableStateOf(false) }
    var isPositionRestored by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    suspend fun loadData() {
        uiState = UiState.Loading
        isPositionRestored = false
        try {
            // Single-pass streaming read & SHA-256 fingerprinting
            val loaded = safRepository.loadDocument(documentUri)
            // Offload parsing and heading index generation to Dispatchers.Default
            val parsed = withContext(Dispatchers.Default) {
                MarkdownBlockParser().parseDocument(loaded.content)
            }
            uiState = UiState.Success(loaded.metadata to parsed)
        } catch (e: Exception) {
            uiState = UiState.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    LaunchedEffect(documentUri) {
        loadData()
    }

    val successState = (uiState as? UiState.Success<Pair<DocumentMetadata?, ParsedDocument>>)
    val parsedDoc = successState?.data?.second ?: ParsedDocument()
    val blocks = parsedDoc.blocks
    val headings = parsedDoc.headings
    val headingIndices = parsedDoc.headingIndices
    val headingPositions = parsedDoc.headingPositions
    val metadata: DocumentMetadata? = successState?.data?.first

    // Restore scroll position safely once per document load
    LaunchedEffect(successState) {
        val finger = metadata?.fingerprint
        if (finger != null && blocks.isNotEmpty() && !isPositionRestored) {
            val savedOffset = preferencesRepository.getScrollPosition(finger).first()
            if (savedOffset in 0 until blocks.size) {
                try {
                    listState.scrollToItem(savedOffset, 0)
                } catch (_: Exception) {
                }
            }
            isPositionRestored = true
        }
    }

    // Persist scroll position with debounce, ONLY when position has already been restored and not in TOC jump
    LaunchedEffect(listState.firstVisibleItemIndex, isPositionRestored, isNavigatingByToc) {
        val finger = metadata?.fingerprint
        if (finger != null && isPositionRestored && !isNavigatingByToc) {
            delay(400)
            preferencesRepository.saveScrollPosition(finger, listState.firstVisibleItemIndex, null)
        }
    }

    val activeHeadingId by remember(headingPositions) {
        derivedStateOf {
            val current = listState.firstVisibleItemIndex
            headingPositions.lastOrNull { it.first <= current }?.second
        }
    }

    val activeHeadingText by remember(headings, activeHeadingId) {
        derivedStateOf {
            headings.firstOrNull { it.id == activeHeadingId }?.text
        }
    }

    // Debounced search evaluation off the main thread
    var matchingIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    LaunchedEffect(searchQuery, blocks) {
        if (searchQuery.isBlank()) {
            matchingIndices = emptyList()
        } else {
            delay(250) // Debounce typing
            val query = searchQuery
            matchingIndices = withContext(Dispatchers.Default) {
                blocks.mapIndexedNotNull { index, block ->
                    if (block.containsText(query)) index else null
                }
            }
        }
    }

    // Safe jump: clamps the target against live list bounds.
    // Long-distance jumps (>30 items) use instant scroll to avoid composing intermediate blocks.
    fun scrollToBlockIndex(targetIndex: Int, onDone: () -> Unit = {}) {
        val target = clampScrollTarget(targetIndex, blocks.size)
        coroutineScope.launch {
            try {
                if (shouldUseInstantScroll(target, listState.firstVisibleItemIndex)) {
                    listState.scrollToItem(target, 0)
                } else {
                    listState.animateScrollToItem(target)
                }
            } catch (_: Exception) {
            } finally {
                onDone()
            }
        }
    }

    val rawProgress by remember(blocks) {
        derivedStateOf {
            if (blocks.isEmpty()) 0f
            else (listState.firstVisibleItemIndex.toFloat() / blocks.size.toFloat()).coerceIn(0f, 1f)
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        label = "ReadingProgress"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                TocDrawerContent(
                    headings = headings,
                    activeHeadingId = activeHeadingId,
                    isDrawerOpen = drawerState.isOpen,
                    isNavigating = isNavigatingByToc,
                    onHeadingClick = { headingId ->
                        headingIndices[headingId]?.let { targetBlockIndex ->
                            isNavigatingByToc = true
                            coroutineScope.launch {
                                drawerState.close()
                            }
                            scrollToBlockIndex(targetBlockIndex) {
                                isNavigatingByToc = false
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (isSearchActive) {
                    InDocumentSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it; currentMatchIndex = 0 },
                        matchCount = matchingIndices.size,
                        currentMatchIndex = currentMatchIndex,
                        onNextMatch = {
                            if (matchingIndices.isNotEmpty()) {
                                currentMatchIndex = (currentMatchIndex + 1) % matchingIndices.size
                                scrollToBlockIndex(matchingIndices[currentMatchIndex])
                            }
                        },
                        onPreviousMatch = {
                            if (matchingIndices.isNotEmpty()) {
                                currentMatchIndex = (currentMatchIndex - 1 + matchingIndices.size) % matchingIndices.size
                                scrollToBlockIndex(matchingIndices[currentMatchIndex])
                            }
                        },
                        onCloseSearch = { isSearchActive = false; searchQuery = "" }
                    )
                } else {
                    Column {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = metadata?.displayName ?: "Reading",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val percent = (rawProgress * 100).toInt()
                                    Text(
                                        text = if (blocks.isNotEmpty()) "$percent% read" else "Document",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back to library"
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { coroutineScope.launch { loadData() } }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload document")
                                }

                                if (onThemeChange != null) {
                                    Box {
                                        IconButton(onClick = { showThemeMenu = true }) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = when (themeMode) {
                                                            ReaderThemeMode.SEPIA -> "📖"
                                                            ReaderThemeMode.LIGHT -> "☀️"
                                                            ReaderThemeMode.DARK -> "🌙"
                                                            ReaderThemeMode.SYSTEM -> "⚙️"
                                                        },
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = showThemeMenu,
                                            onDismissRequest = { showThemeMenu = false },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            listOf(
                                                ReaderThemeMode.SEPIA to "Paper (Archival)",
                                                ReaderThemeMode.LIGHT to "Studio (Light)",
                                                ReaderThemeMode.DARK to "Midnight (Dark)",
                                                ReaderThemeMode.SYSTEM to "System Adaptive"
                                            ).forEach { (mode, label) ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = label,
                                                            fontWeight = if (themeMode == mode) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (themeMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        onThemeChange(mode)
                                                        showThemeMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search text")
                                }

                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.List, contentDescription = "Table of contents")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )

                        LinearProgressIndicator(
                            progress = animatedProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                when (val state = uiState) {
                    is UiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Composing document…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is UiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Unable to Open Document",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { coroutineScope.launch { loadData() } },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    is UiState.Success -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 760.dp)
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                            ) {
                                itemsIndexed(
                                    items = blocks,
                                    key = { _, b -> b.id },
                                    contentType = { _, b -> b::class }
                                ) { _, block ->
                                    BlockItemPresenter(block, searchQuery, themeMode)
                                }

                                item {
                                    Spacer(modifier = Modifier.height(48.dp))
                                }
                            }

                            // Isolated floating indicator to prevent parent recomposition
                            FloatingReadingPositionPill(
                                activeHeadingText = activeHeadingText,
                                listState = listState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 20.dp)
                                    .navigationBarsPadding()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingReadingPositionPill(
    activeHeadingText: String?,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    if (activeHeadingText != null && listState.isScrollInProgress) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = activeHeadingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun BlockModel.containsText(query: String): Boolean = when (this) {
    is HeadingBlock -> text.contains(query, true)
    is ParagraphBlock -> text.contains(query, true)
    is CodeBlock -> code.contains(query, true)
    is ListItemBlock -> text.contains(query, true) || children.any { it.containsText(query) }
    is CalloutBlock -> children.any { it.containsText(query) }
    is TableBlock -> headers.any { it.text.contains(query, true) } || rows.any { r -> r.any { it.text.contains(query, true) } }
    else -> false
}
