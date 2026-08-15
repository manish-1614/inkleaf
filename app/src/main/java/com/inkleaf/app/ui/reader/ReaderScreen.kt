package com.inkleaf.app.ui.reader

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkleaf.app.data.preferences.ReaderPreferencesRepository
import com.inkleaf.app.data.saf.DocumentMetadata
import com.inkleaf.app.data.saf.SafDocumentRepository
import com.inkleaf.app.domain.model.*
import com.inkleaf.app.domain.parser.MarkdownBlockParser
import com.inkleaf.app.ui.theme.ReaderThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    // DocumentMetadata? because getDocumentMetadata returns null when the SAF URI cannot be resolved.
    var uiState by remember { mutableStateOf<UiState<Pair<DocumentMetadata?, List<BlockModel>>>>(UiState.Loading) }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var currentMatchIndex by remember { mutableStateOf(0) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    suspend fun loadData() {
        uiState = UiState.Loading
        try {
            val metadata = safRepository.getDocumentMetadata(documentUri)
            val rawContent = safRepository.readDocumentContent(documentUri)
            val blocks = MarkdownBlockParser().parseToBlocks(rawContent)
            uiState = UiState.Success(metadata to blocks)
        } catch (e: Exception) {
            uiState = UiState.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    LaunchedEffect(documentUri) {
        loadData()
    }

    val successState = (uiState as? UiState.Success<Pair<DocumentMetadata?, List<BlockModel>>>)
    val blocks = successState?.data?.second ?: emptyList()
    val metadata: DocumentMetadata? = successState?.data?.first

    val headings = remember(blocks) { blocks.filterIsInstance<HeadingBlock>() }
    val headingIndices = remember(blocks) { headingIndexById(blocks) }
    val headingPositions = remember(blocks) {
        blocks.mapIndexedNotNull { index, block -> if (block is HeadingBlock) index to block.id else null }
    }

    LaunchedEffect(successState, listState) {
        val finger = metadata?.fingerprint
        if (finger != null && blocks.isNotEmpty()) {
            val savedOffset = preferencesRepository.getScrollPosition(finger).first()
            if (savedOffset in 0 until blocks.size) {
                listState.scrollToItem(savedOffset)
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val finger = metadata?.fingerprint
        if (finger != null) {
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

    val matchingIndices = remember(blocks, searchQuery) {
        if (searchQuery.isEmpty()) emptyList<Int>()
        else blocks.mapIndexedNotNull { index, block ->
            if (block.containsText(searchQuery)) index else null
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TocDrawerContent(
                    headings = headings,
                    activeHeadingId = activeHeadingId,
                    isDrawerOpen = drawerState.isOpen,
                    onHeadingClick = { headingId ->
                        headingIndices[headingId]?.let { targetBlockIndex ->
                            coroutineScope.launch {
                                listState.animateScrollToItem(targetBlockIndex)
                                drawerState.close()
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
                                coroutineScope.launch { listState.animateScrollToItem(matchingIndices[currentMatchIndex]) }
                            }
                        },
                        onPreviousMatch = {
                            if (matchingIndices.isNotEmpty()) {
                                currentMatchIndex = (currentMatchIndex - 1 + matchingIndices.size) % matchingIndices.size
                                coroutineScope.launch { listState.animateScrollToItem(matchingIndices[currentMatchIndex]) }
                            }
                        },
                        onCloseSearch = { isSearchActive = false; searchQuery = "" }
                    )
                } else {
                    TopAppBar(
                        title = { Text(metadata?.displayName ?: "Reading", fontSize = 18.sp) },
                        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                        actions = {
                            IconButton(onClick = { coroutineScope.launch { loadData() } }) { Icon(Icons.Default.Refresh, null) }
                            IconButton(onClick = { isSearchActive = true }) { Icon(Icons.Default.Search, null) }
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) { Icon(Icons.Default.List, null) }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
                when (val state = uiState) {
                    is UiState.Loading -> {
                        // Full-document loading skeleton while SAF reads on Dispatchers.IO
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                    is UiState.Error -> {
                        // Failure isolation — never crash, always present a recoverable error state
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Could not open document",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { coroutineScope.launch { loadData() } }) {
                                Text("Retry")
                            }
                        }
                    }
                    is UiState.Success -> {
                        val scrollProgress by remember(blocks) {
                            derivedStateOf {
                                if (blocks.isEmpty()) 0f
                                else listState.firstVisibleItemIndex.toFloat() / blocks.size.toFloat()
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            itemsIndexed(
                                items = blocks,
                                key = { _, b -> b.id },
                                contentType = { _, b -> b::class }
                            ) { _, block ->
                                BlockItemPresenter(block, searchQuery, themeMode)
                            }
                        }

                        // Thin reading progress indicator at top of the content viewport
                        LinearProgressIndicator(
                            progress = scrollProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.TopCenter),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
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
