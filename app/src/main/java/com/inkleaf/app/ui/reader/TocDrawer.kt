package com.inkleaf.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkleaf.app.domain.model.HeadingBlock

@Composable
fun TocDrawerContent(
    headings: List<HeadingBlock>,
    activeHeadingId: String?,
    onHeadingClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tocListState = rememberLazyListState()

    LaunchedEffect(activeHeadingId) {
        activeHeadingId?.let { id ->
            val index = headings.indexOfFirst { it.id == id }
            if (index >= 0) {
                tocListState.animateScrollToItem(index)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .padding(16.dp)
    ) {
        Text(
            text = "Table of Contents",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.primary
        )

        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

        if (headings.isEmpty()) {
            Text(
                text = "No headings found in this document.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                state = tocListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = headings,
                    key = { _, heading -> heading.id }
                ) { _, heading ->
                    val isActive = heading.id == activeHeadingId
                    val indent = (heading.level - 1) * 12
                    
                    val textColor = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    val textWeight = if (isActive) {
                        FontWeight.Bold
                    } else if (heading.level == 1) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    }

                    Surface(
                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indent.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onHeadingClick(heading.id) }
                    ) {
                        Text(
                            text = heading.text,
                            fontSize = when (heading.level) {
                                1 -> 16.sp
                                2 -> 14.sp
                                else -> 12.sp
                            },
                            fontWeight = textWeight,
                            color = textColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

