package com.inkleaf.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkleaf.app.R
import com.inkleaf.app.data.preferences.*
import com.inkleaf.app.ui.theme.ReaderThemeMode
import com.inkleaf.app.ui.theme.displayLabel
import kotlinx.coroutines.launch

enum class HomeDocumentTab(
    val label: String,
    val emptyStateText: String
) {
    RECENT("Recent", "No recently opened documents."),
    FAVOURITES("Favourites", "No favourite documents yet.")
}

fun defaultHomeDocumentTab(): HomeDocumentTab = HomeDocumentTab.RECENT

fun homeDocumentTabs(): List<HomeDocumentTab> = listOf(
    HomeDocumentTab.RECENT,
    HomeDocumentTab.FAVOURITES
)

private fun readingSurfaceModes(): List<ReaderThemeMode> = listOf(
    ReaderThemeMode.LIGHT,
    ReaderThemeMode.DARK,
    ReaderThemeMode.SEPIA,
    ReaderThemeMode.SYSTEM
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    preferencesRepository: ReaderPreferencesRepository,
    onDocumentSelect: (Uri) -> Unit,
    currentTheme: ReaderThemeMode,
    onThemeChange: (ReaderThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val recents by preferencesRepository.recentsFlow.collectAsState(initial = emptyList())
    val favorites by preferencesRepository.favoritesFlow.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf(defaultHomeDocumentTab()) }
    val tabs = homeDocumentTabs()

    // SAF File Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                onDocumentSelect(uri)
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.inkleaf_logo),
                            contentDescription = "InkLeaf logo",
                            modifier = Modifier
                                .height(28.dp)
                                .padding(end = 8.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text("Inkleaf Reader", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reserved Advertisement Area",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Welcome back! Tap below to pick a Markdown document from your local storage.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("text/markdown", "text/x-markdown", "text/plain")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Open Markdown Document", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Theme Preferences section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Reading Surface", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            readingSurfaceModes().forEach { mode ->
                                FilterChip(
                                    selected = currentTheme == mode,
                                    onClick = { onThemeChange(mode) },
                                    label = { Text(mode.displayLabel) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                TabRow(
                    selectedTabIndex = tabs.indexOf(selectedTab),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.label, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            when (selectedTab) {
                HomeDocumentTab.RECENT -> {
                    if (recents.isEmpty()) {
                        item {
                            EmptyHomeTabState(selectedTab.emptyStateText)
                        }
                    } else {
                        items(
                            items = recents,
                            key = { it.uriString }
                        ) { item ->
                            RecentCardItem(
                                item = item,
                                onClick = { onDocumentSelect(Uri.parse(item.uriString)) },
                                onDelete = {
                                    coroutineScope.launch {
                                        preferencesRepository.removeRecentDocument(item.uriString)
                                    }
                                },
                                isFavorite = favorites.any { it.uriString == item.uriString },
                                onToggleFavorite = {
                                    coroutineScope.launch {
                                        preferencesRepository.toggleFavorite(item.uriString, item.displayName)
                                    }
                                }
                            )
                        }
                    }
                }
                HomeDocumentTab.FAVOURITES -> {
                    if (favorites.isEmpty()) {
                        item {
                            EmptyHomeTabState(selectedTab.emptyStateText)
                        }
                    } else {
                        items(
                            items = favorites,
                            key = { it.uriString }
                        ) { item ->
                            FavoriteCardItem(
                                item = item,
                                onClick = { onDocumentSelect(Uri.parse(item.uriString)) },
                                onRemoveFavorite = {
                                    coroutineScope.launch {
                                        preferencesRepository.toggleFavorite(item.uriString, item.displayName)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHomeTabState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeBrandingFooter() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 180.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.inkleaf_logo),
                contentDescription = "InkLeaf logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentScale = ContentScale.Fit
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                tonalElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Reserved advertisement area",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RecentCardItem(
    item: RecentItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(item.uriString, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Row {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete recent entry")
                }
            }
        }
    }
}

@Composable
fun FavoriteCardItem(
    item: FavoriteItem,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(item.uriString, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = onRemoveFavorite) {
                Icon(Icons.Default.Favorite, contentDescription = "Remove favorite", tint = Color.Red)
            }
        }
    }
}
