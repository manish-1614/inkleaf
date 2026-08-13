package com.inkleaf.app.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkleaf.app.domain.model.HeadingBlock

@Composable
fun TocDrawerContent(
    headings: List<HeadingBlock>,
    onHeadingClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = headings,
                    key = { _, heading -> heading.id }
                ) { _, heading ->
                    val indent = (heading.level - 1) * 12
                    Text(
                        text = heading.text,
                        fontSize = when (heading.level) {
                            1 -> 16.sp
                            2 -> 14.sp
                            else -> 12.sp
                        },
                        fontWeight = if (heading.level == 1) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indent.dp)
                            .clickable { onHeadingClick(heading.id) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
