package com.inkleaf.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.inkleaf.app.domain.model.*
import com.inkleaf.app.domain.parser.SyntaxHighlighter
import com.inkleaf.app.domain.parser.normalizeCodeLanguage
import com.inkleaf.app.ui.theme.ReaderThemeMode
import com.inkleaf.app.ui.theme.ReaderThemePalette
import com.inkleaf.app.ui.theme.resolvePalette
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            .padding(vertical = 5.dp)
    ) {
        when (block) {
            is HeadingBlock -> {
                val uriHandler = LocalUriHandler.current
                val annotatedText = renderStyledText(block.runs, block.text, searchQuery, themeMode)
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = when (block.level) {
                                1 -> 24.dp
                                2 -> 18.dp
                                3 -> 14.dp
                                else -> 10.dp
                            },
                            bottom = when (block.level) {
                                1 -> 8.dp
                                2 -> 6.dp
                                else -> 4.dp
                            }
                        )
                ) {
                    val textStyle = when (block.level) {
                        1 -> MaterialTheme.typography.headlineLarge
                        2 -> MaterialTheme.typography.headlineMedium
                        3 -> MaterialTheme.typography.headlineSmall
                        else -> MaterialTheme.typography.titleMedium
                    }
                    val headingColor = when (block.level) {
                        1, 2 -> MaterialTheme.colorScheme.primary
                        3 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    ClickableText(
                        text = annotatedText,
                        style = textStyle.copy(color = headingColor),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    try {
                                        uriHandler.openUri(annotation.item)
                                    } catch (_: Exception) {
                                    }
                                }
                        }
                    )

                    // Subtle editorial accent line under H1
                    if (block.level == 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        )
                    }
                }
            }
            is ParagraphBlock -> {
                val uriHandler = LocalUriHandler.current
                val annotatedText = renderStyledText(block.runs, block.text, searchQuery, themeMode)
                
                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.padding(vertical = 2.dp),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                try {
                                    uriHandler.openUri(annotation.item)
                                } catch (_: Exception) {
                                }
                            }
                    }
                )
            }
            is CodeBlock -> {
                val clipboardManager = LocalClipboardManager.current
                val coroutineScope = rememberCoroutineScope()
                var isCopied by remember { mutableStateOf(false) }

                val scrollState = rememberScrollState()
                val lines = block.code.split("\n")
                val showLineNumbers = lines.size > 1
                val languageLabel = normalizeCodeLanguage(block.language)
                val headerLayout = codeBlockHeaderLayout(languageLabel)

                val codeCardBg = Color(0xFF0F172A)
                val headerBg = Color(0xFF1E293B)
                val lineNumberColor = Color(0xFF64748B)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(codeCardBg)
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBg)
                            .heightIn(min = 40.dp)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (headerLayout.showLanguageBadge && languageLabel != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = languageLabel.uppercase(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "CODE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(block.code))
                                isCopied = true
                                coroutineScope.launch {
                                    delay(2000)
                                    isCopied = false
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isCopied) Color(0xFF34D399) else Color(0xFF93C5FD)
                            )
                        ) {
                            Text(
                                text = if (isCopied) "✓ Copied" else "Copy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFF334155))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            if (showLineNumbers) {
                                Column(
                                    modifier = Modifier.padding(end = 14.dp),
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
                                text = SyntaxHighlighter.highlight(block.code, block.language, searchQuery),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
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
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.width(tableWidth)) {
                        if (block.headers.isNotEmpty()) {
                            TableRow(
                                cells = normalizedTableRow(block.headers, columnCount),
                                columnWidths = columnWidths,
                                searchQuery = searchQuery,
                                isHeader = true,
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                themeMode = themeMode
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        block.rows.forEachIndexed { rowIndex, rowData ->
                            val rowBg = if (rowIndex % 2 == 1) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            } else {
                                Color.Transparent
                            }
                            TableRow(
                                cells = normalizedTableRow(rowData, columnCount),
                                columnWidths = columnWidths,
                                searchQuery = searchQuery,
                                isHeader = false,
                                backgroundColor = rowBg,
                                themeMode = themeMode
                            )
                            if (rowIndex < block.rows.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            }
                        }
                    }
                }
            }
            is CalloutBlock -> {
                val (accentColor, iconSymbol, defaultTitle) = when (block.type) {
                    "NOTE" -> Triple(Color(0xFF2563EB), "ℹ️", "Note")
                    "TIP" -> Triple(Color(0xFF059669), "💡", "Tip")
                    "WARNING" -> Triple(Color(0xFFD97706), "⚠️", "Warning")
                    "CAUTION" -> Triple(Color(0xFFDC2626), "🛑", "Caution")
                    "IMPORTANT" -> Triple(Color(0xFF7C3AED), "📌", "Important")
                    else -> Triple(MaterialTheme.colorScheme.primary, "📌", block.type)
                }
                val cardBg = accentColor.copy(alpha = 0.08f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBg)
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = iconSymbol,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = block.title ?: defaultTitle,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontSize = 14.sp
                            )
                        }
                        
                        block.children.forEach { child ->
                            BlockItemPresenter(
                                block = child,
                                searchQuery = searchQuery,
                                themeMode = themeMode
                            )
                        }
                    }
                }
            }
            is HorizontalRuleBlock -> {
                // Tasteful literary section ornament
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "  •   •   •  ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        letterSpacing = 2.sp
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }
            }
            is MermaidBlock -> {
                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                val resolvedThemeMode = when (themeMode.resolvePalette(isDark)) {
                    com.inkleaf.app.ui.theme.ReaderThemePalette.DARK -> "dark"
                    com.inkleaf.app.ui.theme.ReaderThemePalette.PAPER -> "paper"
                    com.inkleaf.app.ui.theme.ReaderThemePalette.LIGHT -> "light"
                }
                MermaidWebViewPresenter(
                    diagramCode = block.diagramSource,
                    themeMode = resolvedThemeMode
                )
            }
            is MathBlock -> {
                KatexWebViewPresenter(
                    latexFormula = block.latex,
                    isInline = block.isInline
                )
            }
            is SvgBlock -> {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "SVG Graphic Content",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is ImageBlock -> {
                ImageItemPresenter(
                    url = block.url,
                    altText = block.altText,
                    title = block.title,
                    themeMode = themeMode
                )
            }
            is ListItemBlock -> {
                ListItemRow(
                    block = block,
                    searchQuery = searchQuery,
                    themeMode = themeMode
                )
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
fun ImageItemPresenter(
    url: String,
    altText: String?,
    title: String?,
    themeMode: ReaderThemeMode,
    modifier: Modifier = Modifier
) {
    val isDark = themeMode == ReaderThemeMode.DARK
    val placeholderBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val placeholderBorder = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = altText ?: "Markdown Image",
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(placeholderBg, RoundedCornerShape(10.dp))
                        .border(1.dp, placeholderBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            },
            error = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(placeholderBg, RoundedCornerShape(10.dp))
                        .border(1.dp, placeholderBorder, RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🖼️ Image Reference",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = altText ?: url,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = textColor.copy(alpha = 0.85f)
                    )
                    if (title != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.65f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                    ) {
                        Text(
                            text = "Offline Mode / Remote Source",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        )
    }
}

@Composable
private fun ListItemRow(
    block: ListItemBlock,
    searchQuery: String,
    themeMode: ReaderThemeMode
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (block.level * 16).dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .padding(top = 3.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (block.isTask) {
                val checkboxColor = when (themeMode) {
                    ReaderThemeMode.DARK -> Color(0xFF93C5FD)
                    else -> MaterialTheme.colorScheme.primary
                }
                val boxBorderColor = if (block.isChecked) checkboxColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (block.isChecked) checkboxColor.copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.5.dp, boxBorderColor, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (block.isChecked) {
                        Text(
                            "✓",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = checkboxColor,
                            lineHeight = 10.sp
                        )
                    }
                }
            } else if (block.isOrdered) {
                Text(
                    text = "${block.number}.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                val bulletMarker = when (block.level % 3) {
                    0 -> "•"
                    1 -> "◦"
                    else -> "▪"
                }
                Text(
                    text = bulletMarker,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        val uriHandler = LocalUriHandler.current
        val annotatedText = renderStyledText(block.runs, block.text, searchQuery, themeMode)
        val textStyle = if (block.isTask && block.isChecked) {
            MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textDecoration = TextDecoration.LineThrough
            )
        } else {
            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
        }
        
        Column(modifier = Modifier.weight(1f)) {
            ClickableText(
                text = annotatedText,
                style = textStyle,
                onClick = { offset ->
                    annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            try {
                                uriHandler.openUri(annotation.item)
                            } catch (_: Exception) {
                            }
                        }
                }
            )
            if (block.children.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                block.children.forEach { child ->
                    BlockItemPresenter(
                        block = child,
                        searchQuery = searchQuery,
                        themeMode = themeMode
                    )
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<TableCellModel>,
    columnWidths: List<Float>,
    searchQuery: String,
    isHeader: Boolean,
    backgroundColor: Color,
    themeMode: ReaderThemeMode
) {
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxWidth()
    ) {
        cells.forEachIndexed { index, cell ->
            Box(
                modifier = Modifier
                    .width(columnWidths[index].dp)
                    .padding(horizontal = 12.dp, vertical = if (isHeader) 10.dp else 8.dp)
            ) {
                val textAlign = when (cell.alignment) {
                    TableCellAlignment.LEFT -> TextAlign.Start
                    TableCellAlignment.CENTER -> TextAlign.Center
                    TableCellAlignment.RIGHT -> TextAlign.End
                }
                val annotatedText = renderStyledText(cell.runs, cell.text, searchQuery, themeMode)
                Text(
                    text = annotatedText,
                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (isHeader) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

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

@Composable
fun renderStyledText(
    runs: List<StyledTextRun>,
    fallbackText: String,
    searchQuery: String,
    themeMode: ReaderThemeMode
): AnnotatedString {
    if (runs.isEmpty()) {
        return highlightSearch(fallbackText, searchQuery)
    }
    return remember(runs, searchQuery, themeMode) {
        val b = AnnotatedString.Builder()
        runs.forEach { run ->
            val start = b.length
            b.append(run.text)
            val end = b.length

            val (codeBg, codeColor) = when (themeMode) {
                ReaderThemeMode.DARK -> Color(0xFF1E293B) to Color(0xFF93C5FD)
                ReaderThemeMode.SEPIA -> Color(0xFFE2DAC8) to Color(0xFF573A25)
                else -> Color(0xFFE2E8F0) to Color(0xFF1E293B)
            }

            val (highlightBg, highlightColor) = when (themeMode) {
                ReaderThemeMode.DARK -> Color(0xFF78350F) to Color(0xFFFDE68A)
                ReaderThemeMode.SEPIA -> Color(0xFFFDE047) to Color(0xFF451A03)
                else -> Color(0xFFFEF08A) to Color(0xFF713F12)
            }

            val textDecor = when {
                run.isStrikethrough && run.isUnderline -> TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline))
                run.isStrikethrough -> TextDecoration.LineThrough
                run.isUnderline -> TextDecoration.Underline
                else -> TextDecoration.None
            }

            val baseShift = when {
                run.isSubscript -> BaselineShift.Subscript
                run.isSuperscript -> BaselineShift.Superscript
                else -> BaselineShift.None
            }

            val fontSize = when {
                run.isSubscript || run.isSuperscript -> 10.sp
                else -> TextUnit.Unspecified
            }

            val style = SpanStyle(
                fontWeight = if (run.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = textDecor,
                fontFamily = if (run.isCode || run.isKbd) FontFamily.Monospace else FontFamily.Default,
                color = when {
                    run.linkUrl != null -> when (themeMode) {
                        ReaderThemeMode.DARK -> Color(0xFF38BDF8)
                        ReaderThemeMode.SEPIA -> Color(0xFF8D3E1B)
                        else -> Color(0xFF0F5B78)
                    }
                    run.isHighlighted -> highlightColor
                    run.isCode -> codeColor
                    else -> Color.Unspecified
                },
                background = when {
                    run.isHighlighted -> highlightBg
                    run.isCode || run.isKbd -> codeBg
                    else -> Color.Transparent
                },
                baselineShift = baseShift,
                fontSize = fontSize
            )
            b.addStyle(style, start, end)

            if (run.linkUrl != null) {
                b.addStringAnnotation(
                    tag = "URL",
                    annotation = run.linkUrl,
                    start = start,
                    end = end
                )
            }
        }

        val fullText = b.toAnnotatedString().text
        if (searchQuery.isNotEmpty()) {
            var startIndex = fullText.indexOf(searchQuery, ignoreCase = true)
            while (startIndex != -1) {
                val endIndex = startIndex + searchQuery.length
                b.addStyle(
                    style = SpanStyle(
                        background = Color.Yellow.copy(alpha = 0.6f),
                        color = Color.Black
                    ),
                    start = startIndex,
                    end = endIndex
                )
                startIndex = fullText.indexOf(searchQuery, startIndex + 1, ignoreCase = true)
            }
        }
        b.toAnnotatedString()
    }
}
