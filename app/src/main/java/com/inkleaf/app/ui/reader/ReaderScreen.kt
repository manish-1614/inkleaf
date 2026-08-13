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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkleaf.app.data.preferences.ReaderPreferencesRepository
import com.inkleaf.app.data.saf.DocumentMetadata
import com.inkleaf.app.data.saf.SafDocumentRepository
import com.inkleaf.app.domain.model.HeadingBlock
import com.inkleaf.app.domain.parser.MarkdownBlockParser
import com.inkleaf.app.ui.theme.ReaderThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    var metadata by remember { mutableStateOf<DocumentMetadata?>(null) }
    var rawContent by remember { mutableStateOf("") }
    val blocks = remember(rawContent) { MarkdownBlockParser().parseToBlocks(rawContent) }
    
    val listState = rememberLazyListState()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var currentMatchIndex by remember { mutableStateOf(0) }
    
    // Find all blocks matching the search query
    val matchingIndices = remember(blocks, searchQuery) {
        if (searchQuery.isEmpty()) emptyList<Int>()
        else {
            blocks.mapIndexedNotNull { index, block ->
                val match = when (block) {
                    is com.inkleaf.app.domain.model.HeadingBlock -> block.text.contains(searchQuery, ignoreCase = true)
                    is com.inkleaf.app.domain.model.ParagraphBlock -> block.text.contains(searchQuery, ignoreCase = true)
                    is com.inkleaf.app.domain.model.CodeBlock -> block.code.contains(searchQuery, ignoreCase = true)
                    is com.inkleaf.app.domain.model.CalloutBlock -> block.content.contains(searchQuery, ignoreCase = true)
                    else -> false
                }
                if (match) index else null
            }
        }
    }

    // Scroll progress calculation: current first visible index / total items count
    val scrollProgress by remember {
        derivedStateOf {
            if (blocks.isEmpty()) 0f
            else {
                val visibleIndex = listState.firstVisibleItemIndex
                val totalItems = blocks.size
                visibleIndex.toFloat() / totalItems.toFloat()
            }
        }
    }

    // Load document content
    LaunchedEffect(documentUri) {
        metadata = safRepository.getDocumentMetadata(documentUri)
        rawContent = safRepository.readDocumentContent(documentUri)
    }

    // Restore scroll position on document load
    LaunchedEffect(rawContent, metadata) {
        val finger = metadata?.fingerprint
        if (finger != null && rawContent.isNotEmpty()) {
            val savedOffset = preferencesRepository.getScrollPosition(finger).first()
            if (savedOffset in 0 until blocks.size) {
                listState.scrollToItem(savedOffset)
            }
        }
    }

    // Auto-save scroll position when scrolled
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val finger = metadata?.fingerprint
        if (finger != null) {
            preferencesRepository.saveScrollPosition(finger, listState.firstVisibleItemIndex, null)
        }
    }

    val headings = remember(blocks) {
        blocks.filterIsInstance<HeadingBlock>()
    }
    val headingIndices = remember(blocks) { headingIndexById(blocks) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TocDrawerContent(
                    headings = headings,
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
            topBar = {
                if (isSearchActive) {
                    InDocumentSearchBar(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                            currentMatchIndex = 0
                        },
                        matchCount = matchingIndices.size,
                        currentMatchIndex = currentMatchIndex,
                        onNextMatch = {
                            if (matchingIndices.isNotEmpty()) {
                                currentMatchIndex = (currentMatchIndex + 1) % matchingIndices.size
                                coroutineScope.launch {
                                    listState.animateScrollToItem(matchingIndices[currentMatchIndex])
                                }
                            }
                        },
                        onPreviousMatch = {
                            if (matchingIndices.isNotEmpty()) {
                                currentMatchIndex = (currentMatchIndex - 1 + matchingIndices.size) % matchingIndices.size
                                coroutineScope.launch {
                                    listState.animateScrollToItem(matchingIndices[currentMatchIndex])
                                }
                            }
                        },
                        onCloseSearch = {
                            isSearchActive = false
                            searchQuery = ""
                        }
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = metadata?.displayName ?: "Reading document",
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.List, contentDescription = "Table of Contents", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    itemsIndexed(
                        items = blocks,
                        key = { _, block -> block.id },
                        contentType = { _, block -> block::class }
                    ) { _, block ->
                        BlockItemPresenter(
                            block = block,
                            searchQuery = searchQuery,
                            themeMode = themeMode
                        )
                    }
                }

                // Thin reading progress indicator at top of page viewport
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
