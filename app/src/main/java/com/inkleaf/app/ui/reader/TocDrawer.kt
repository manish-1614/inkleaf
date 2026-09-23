package com.inkleaf.app.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkleaf.app.domain.model.HeadingBlock

@Composable
fun TocDrawerContent(
    headings: List<HeadingBlock>,
    activeHeadingId: String?,
    isDrawerOpen: Boolean,
    onHeadingClick: (String) -> Unit,
    isNavigating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tocListState = rememberLazyListState()

    LaunchedEffect(activeHeadingId, isDrawerOpen, isNavigating) {
        if (!isDrawerOpen || isNavigating) return@LaunchedEffect
        activeHeadingId?.let { id ->
            val index = headings.indexOfFirst { it.id == id }
            if (index in 0 until headings.size) {
                try {
                    tocListState.animateScrollToItem(index)
                } catch (_: Exception) {
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        // Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Contents",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (headings.isNotEmpty()) "${headings.size} sections" else "No headings",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(26.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = "OUTLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (headings.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "📄",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No headings found in this document.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = tocListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = headings,
                    key = { _, heading -> heading.id }
                ) { _, heading ->
                    val isActive = heading.id == activeHeadingId
                    val indent = when (heading.level) {
                        1 -> 0.dp
                        2 -> 14.dp
                        3 -> 26.dp
                        else -> 36.dp
                    }

                    Surface(
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indent)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isNavigating) { onHeadingClick(heading.id) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 10.dp)
                        ) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 3.dp, height = 18.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (heading.level == 1) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Text(
                                text = heading.text,
                                style = when (heading.level) {
                                    1 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    2 -> MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold)
                                    else -> MaterialTheme.typography.bodySmall
                                },
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else if (heading.level == 1) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
