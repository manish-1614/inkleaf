package com.inkleaf.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkleaf.app.domain.model.*
import com.inkleaf.app.domain.parser.normalizeCodeLanguage
import com.inkleaf.app.ui.theme.ReaderThemeMode

enum class CodeBlockCopyAnchor {
    End
}

data class CodeBlockHeaderLayout(
    val showLanguageBadge: Boolean,
    val copyAnchor: CodeBlockCopyAnchor
)

fun codeBlockHeaderLayout(languageLabel: String?): CodeBlockHeaderLayout {
    return CodeBlockHeaderLayout(
        showLanguageBadge = languageLabel != null,
        copyAnchor = CodeBlockCopyAnchor.End
    )
}

@Composable
fun BlockItemPresenter(
    block: BlockModel,
    searchQuery: String,
    themeMode: ReaderThemeMode,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        when (block) {
            is HeadingBlock -> {
                val textStyle = when (block.level) {
                    1 -> MaterialTheme.typography.headlineLarge
                    2 -> MaterialTheme.typography.headlineMedium
                    3 -> MaterialTheme.typography.headlineSmall
                    else -> MaterialTheme.typography.titleMedium
                }
                val color = if (block.level <= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                Text(
                    text = highlightSearch(block.text, searchQuery),
                    style = textStyle,
                    color = color,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                )
            }
            is ParagraphBlock -> {
                Text(
                    text = highlightSearch(block.text, searchQuery),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            is CodeBlock -> {
                val clipboardManager = LocalClipboardManager.current
                val scrollState = rememberScrollState()
                val lines = block.code.split("\n")
                val showLineNumbers = lines.size > 1
                val languageLabel = normalizeCodeLanguage(block.language)
                val headerLayout = codeBlockHeaderLayout(languageLabel)

                // Dedicated Dark Terminal Container across all theme modes for high-contrast syntax display
                val codeCardBg = Color(0xFF0F172A)
                val headerBg = Color(0xFF1E293B)
                val codeTextColor = Color(0xFFF1F5F9)
                val lineNumberColor = Color(0xFF64748B)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(codeCardBg)
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                ) {
                    // Header Bar with Language Badge and Copy Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBg)
                            .heightIn(min = 40.dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (headerLayout.showLanguageBadge && languageLabel != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF2563EB),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = languageLabel.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { clipboardManager.setText(AnnotatedString(block.code)) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Copy", fontSize = 11.sp, color = Color(0xFF93C5FD))
                        }
                    }
                    HorizontalDivider(color = Color(0xFF334155))

                    // Code Body with Horizontal Scroll & Optional Line Numbers
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (showLineNumbers) {
                                Column(
                                    modifier = Modifier.padding(end = 12.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    lines.indices.forEach { index ->
                                        Text(
                                            text = "${index + 1}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            color = lineNumberColor
                                        )
                                    }
                                }
                            }
                            Text(
                                text = highlightSearch(block.code, searchQuery),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = codeTextColor
                            )
                        }
                    }
                }
            }
            is TableBlock -> {
                val columnWidths = remember(block.headers, block.rows) {
                    tableColumnWidths(block.headers, block.rows)
                }
                val columnCount = columnWidths.size
                val tableWidth = columnWidths.sum().dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.width(tableWidth)) {
                        if (block.headers.isNotEmpty()) {
                            TableRow(
                                cells = normalizedTableRow(block.headers, columnCount),
                                columnWidths = columnWidths,
                                searchQuery = searchQuery,
                                isHeader = true,
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        block.rows.forEachIndexed { rowIndex, rowData ->
                            val rowBg = if (rowIndex % 2 == 1) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            } else {
                                Color.Transparent
                            }
                            TableRow(
                                cells = normalizedTableRow(rowData, columnCount),
                                columnWidths = columnWidths,
                                searchQuery = searchQuery,
                                isHeader = false,
                                backgroundColor = rowBg
                            )
                        }
                    }
                }
            }
            is CalloutBlock -> {
                val accentColor = when (block.type) {
                    "NOTE" -> Color(0xFF2563EB)
                    "TIP" -> Color(0xFF059669)
                    "WARNING" -> Color(0xFFD97706)
                    "CAUTION" -> Color(0xFFDC2626)
                    else -> MaterialTheme.colorScheme.primary
                }
                val cardBg = accentColor.copy(alpha = 0.08f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardBg)
                        .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .intrinsicMinHeight()
                ) {
                    // Left Accent Indicator Bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (block.title != null) {
                            Text(
                                text = block.title,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = highlightSearch(block.content, searchQuery),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            is HorizontalRuleBlock -> {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            is MermaidBlock -> {
                MermaidWebViewPresenter(
                    diagramCode = block.diagramSource,
                    themeMode = themeMode.name.lowercase()
                )
            }
            is MathBlock -> {
                KatexWebViewPresenter(
                    latexFormula = block.latex,
                    isInline = block.isInline
                )
            }
            is SvgBlock -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SVG Graphic content")
                }
            }
            is RawFallbackBlock -> {
                Text(
                    text = highlightSearch(block.rawText, searchQuery),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    columnWidths: List<Float>,
    searchQuery: String,
    isHeader: Boolean,
    backgroundColor: Color
) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .background(backgroundColor)
    ) {
        cells.forEachIndexed { index, cell ->
            Box(
                modifier = Modifier
                    .width(columnWidths[index].dp)
                    .fillMaxHeight()
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    .padding(horizontal = 12.dp, vertical = if (isHeader) 10.dp else 8.dp)
            ) {
                Text(
                    text = highlightSearch(cell, searchQuery),
                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (isHeader) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

// Helper Modifier extension for intrinsic height in Row child
private fun Modifier.intrinsicMinHeight(): Modifier = this.height(IntrinsicSize.Min)

// Basic search highlight helper converting matching queries into highlighted/colored styling
fun highlightSearch(text: String, query: String): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(text)
    val builder = AnnotatedString.Builder(text)
    var startIndex = text.indexOf(query, ignoreCase = true)
    while (startIndex != -1) {
        val endIndex = startIndex + query.length
        builder.addStyle(
            style = SpanStyle(
                background = Color.Yellow.copy(alpha = 0.6f),
                color = Color.Black
            ),
            start = startIndex,
            end = endIndex
        )
        startIndex = text.indexOf(query, startIndex + 1, ignoreCase = true)
    }
    return builder.toAnnotatedString()
}
