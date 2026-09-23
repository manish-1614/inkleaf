package com.inkleaf.app.domain.parser

import com.inkleaf.app.domain.model.*
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.ext.gfm.tables.TableBlock as GfmTableBlock
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.strikethrough.Strikethrough

data class ParsedDocument(
    val blocks: List<BlockModel> = emptyList(),
    val headings: List<HeadingBlock> = emptyList(),
    val headingIndices: Map<String, Int> = emptyMap(),
    val headingPositions: List<Pair<Int, String>> = emptyList()
)

class MarkdownBlockParser {

    private val parser: Parser = Parser.builder()
        .extensions(listOf(TablesExtension.create(), StrikethroughExtension.create()))
        .build()

    fun parseDocument(markdownSource: String): ParsedDocument {
        val blocks = parseToBlocks(markdownSource)
        val headings = mutableListOf<HeadingBlock>()
        val headingIndices = mutableMapOf<String, Int>()
        val headingPositions = mutableListOf<Pair<Int, String>>()

        blocks.forEachIndexed { index, block ->
            if (block is HeadingBlock) {
                headings.add(block)
                headingIndices[block.id] = index
                headingPositions.add(index to block.id)
            }
        }

        return ParsedDocument(
            blocks = blocks,
            headings = headings,
            headingIndices = headingIndices,
            headingPositions = headingPositions
        )
    }

    fun parseToBlocks(markdownSource: String): List<BlockModel> {
        val document = parser.parse(markdownSource)
        var blockIdCounter = 0
        fun nextId() = "block_${blockIdCounter++}"

        return parseChildren(document, 0, ::nextId)
    }

    private fun parseChildren(container: Node, level: Int, nextId: () -> String): List<BlockModel> {
        val blocks = mutableListOf<BlockModel>()
        var child = container.firstChild
        while (child != null) {
            blocks.addAll(parseNode(child, level, nextId))
            child = child.next
        }
        return blocks
    }

    private fun parseNode(node: Node, level: Int, nextId: () -> String): List<BlockModel> {
        val blocks = mutableListOf<BlockModel>()
        val range = SourceRange(0, 0) // Placeholder for range detection

        when (node) {
            is Heading -> {
                val textContent = getTextContent(node)
                val runs = parseRuns(node)
                blocks.add(
                    HeadingBlock(
                        id = nextId(),
                        sourceRange = range,
                        level = node.level,
                        text = textContent,
                        runs = runs
                    )
                )
            }
            is Paragraph -> {
                // Check if it's a standalone image block
                val firstChild = node.firstChild
                if (firstChild is Image && firstChild.next == null) {
                    val altText = getTextContent(firstChild)
                    blocks.add(
                        ImageBlock(
                            id = nextId(),
                            sourceRange = range,
                            url = firstChild.destination ?: "",
                            altText = altText.ifEmpty { null },
                            title = firstChild.title?.ifEmpty { null }
                        )
                    )
                } else {
                    val textContent = getTextContent(node)
                    val runs = parseRuns(node)
                    blocks.add(
                        ParagraphBlock(
                            id = nextId(),
                            sourceRange = range,
                            text = textContent,
                            runs = runs
                        )
                    )
                }
            }
            is FencedCodeBlock -> {
                val info = normalizedFenceInfo(node.info)
                val literal = node.literal ?: ""
                when (info) {
                    "mermaid" -> {
                        blocks.add(
                            MermaidBlock(
                                id = nextId(),
                                sourceRange = range,
                                diagramSource = literal
                            )
                        )
                    }
                    "math", "latex" -> {
                        blocks.add(
                            MathBlock(
                                id = nextId(),
                                sourceRange = range,
                                latex = literal,
                                isInline = false
                            )
                        )
                    }
                    else -> {
                        blocks.add(
                            CodeBlock(
                                id = nextId(),
                                sourceRange = range,
                                language = normalizeCodeLanguage(info),
                                code = literal
                            )
                        )
                    }
                }
            }
            is BlockQuote -> {
                val children = parseChildren(node, level, nextId)
                var type = "QUOTE"
                var title: String? = null
                var finalChildren = children

                if (children.isNotEmpty() && children[0] is ParagraphBlock) {
                    val firstPara = children[0] as ParagraphBlock
                    val matchResult = Regex("""^\s*\[!(NOTE|TIP|WARNING|CAUTION)\](.*)""", RegexOption.DOT_MATCHES_ALL)
                        .find(firstPara.text)
                    if (matchResult != null) {
                        type = matchResult.groupValues[1]
                        title = type
                        val content = matchResult.groupValues[2].trim()

                        var cleanedRuns = firstPara.runs
                        if (firstPara.runs.isNotEmpty()) {
                            val calloutRegex = Regex("""^\s*\[!(?:NOTE|TIP|WARNING|CAUTION)\]\s*(.*)""", RegexOption.DOT_MATCHES_ALL)
                            val firstRunMatch = calloutRegex.find(firstPara.runs[0].text)
                            if (firstRunMatch != null) {
                                val mutableRuns = firstPara.runs.toMutableList()
                                mutableRuns[0] = firstPara.runs[0].copy(text = firstRunMatch.groupValues[1])
                                cleanedRuns = mutableRuns
                            }
                        }
                        val updatedFirstPara = firstPara.copy(text = content, runs = cleanedRuns)
                        finalChildren = listOf(updatedFirstPara) + children.drop(1)
                    }
                }

                blocks.add(
                    CalloutBlock(
                        id = nextId(),
                        sourceRange = range,
                        type = type,
                        title = title,
                        children = finalChildren
                    )
                )
            }
            is GfmTableBlock -> {
                val headers = mutableListOf<TableCellModel>()
                val rows = mutableListOf<List<TableCellModel>>()

                var tablePart = node.firstChild
                while (tablePart != null) {
                    if (tablePart is TableHead) {
                        var row = tablePart.firstChild
                        while (row != null) {
                            if (row is TableRow) {
                                var cell = row.firstChild
                                while (cell != null) {
                                    if (cell is TableCell) {
                                        val align = when (cell.alignment) {
                                            TableCell.Alignment.LEFT -> TableCellAlignment.LEFT
                                            TableCell.Alignment.CENTER -> TableCellAlignment.CENTER
                                            TableCell.Alignment.RIGHT -> TableCellAlignment.RIGHT
                                            else -> TableCellAlignment.LEFT
                                        }
                                        headers.add(TableCellModel(getTextContent(cell), parseRuns(cell), align))
                                    }
                                    cell = cell.next
                                }
                            }
                            row = row.next
                        }
                    } else {
                        var row = tablePart.firstChild
                        while (row != null) {
                            if (row is TableRow) {
                                val rowData = mutableListOf<TableCellModel>()
                                var cell = row.firstChild
                                while (cell != null) {
                                    if (cell is TableCell) {
                                        val align = when (cell.alignment) {
                                            TableCell.Alignment.LEFT -> TableCellAlignment.LEFT
                                            TableCell.Alignment.CENTER -> TableCellAlignment.CENTER
                                            TableCell.Alignment.RIGHT -> TableCellAlignment.RIGHT
                                            else -> TableCellAlignment.LEFT
                                        }
                                        rowData.add(TableCellModel(getTextContent(cell), parseRuns(cell), align))
                                    }
                                    cell = cell.next
                                }
                                rows.add(rowData)
                            }
                            row = row.next
                        }
                    }
                    tablePart = tablePart.next
                }
                blocks.add(
                    TableBlock(
                        id = nextId(),
                        sourceRange = range,
                        headers = headers,
                        rows = rows
                    )
                )
            }
            is ThematicBreak -> {
                blocks.add(
                    HorizontalRuleBlock(
                        id = nextId(),
                        sourceRange = range
                    )
                )
            }
            is BulletList -> {
                blocks.addAll(parseList(node, level, nextId))
            }
            is OrderedList -> {
                blocks.addAll(parseList(node, level, nextId))
            }
            else -> {
                val content = getTextContent(node)
                if (content.isNotBlank()) {
                    blocks.add(
                        RawFallbackBlock(
                            id = nextId(),
                            sourceRange = range,
                            rawText = content
                        )
                    )
                }
            }
        }
        return blocks
    }

    private fun parseList(
        listNode: ListBlock,
        level: Int,
        nextId: () -> String
    ): List<BlockModel> {
        val blocks = mutableListOf<BlockModel>()
        val isOrdered = listNode is OrderedList
        var itemNumber = if (isOrdered) (listNode as OrderedList).startNumber else 1

        var listItem = listNode.firstChild
        while (listItem != null) {
            if (listItem is ListItem) {
                // Parse nested block children of ListItem (level is incremented for nested blocks)
                val childBlocks = parseChildren(listItem, level + 1, nextId)

                var mainText = ""
                var mainRuns = emptyList<StyledTextRun>()
                val remainingChildren = mutableListOf<BlockModel>()

                var isFirstParagraph = true
                for (childBlock in childBlocks) {
                    if (childBlock is ParagraphBlock && isFirstParagraph) {
                        mainText = childBlock.text
                        mainRuns = childBlock.runs
                        isFirstParagraph = false
                    } else {
                        remainingChildren.add(childBlock)
                    }
                }

                var isTask = false
                var isChecked = false
                var cleanedText = mainText
                var cleanedRuns = mainRuns

                if (mainRuns.isNotEmpty()) {
                    val firstRunText = mainRuns[0].text
                    val taskRegex = Regex("""^\[([ xX])\]\s*(.*)""", RegexOption.DOT_MATCHES_ALL)
                    val match = taskRegex.find(firstRunText)
                    if (match != null) {
                        isTask = true
                        isChecked = match.groupValues[1].lowercase() == "x"
                        val remainingText = match.groupValues[2]

                        val mutableRuns = mainRuns.toMutableList()
                        mutableRuns[0] = mainRuns[0].copy(text = remainingText)
                        cleanedRuns = mutableRuns

                        val plainMatch = taskRegex.find(mainText)
                        if (plainMatch != null) {
                            cleanedText = plainMatch.groupValues[2]
                        }
                    }
                }

                blocks.add(
                    ListItemBlock(
                        id = nextId(),
                        sourceRange = SourceRange(0, 0),
                        text = cleanedText,
                        level = level,
                        isOrdered = isOrdered,
                        number = if (isOrdered) itemNumber else null,
                        isTask = isTask,
                        isChecked = isChecked,
                        runs = cleanedRuns,
                        children = remainingChildren
                    )
                )

                if (isOrdered) {
                    itemNumber++
                }
            }
            listItem = listItem.next
        }
        return blocks
    }

    private fun parseRuns(node: Node): List<StyledTextRun> {
        val runs = mutableListOf<StyledTextRun>()

        var isHighlighted = false
        var isUnderline = false
        var isSubscript = false
        var isSuperscript = false
        var isKbd = false

        fun visit(currentNode: Node, isBold: Boolean, isItalic: Boolean, isStrikethrough: Boolean, isCode: Boolean, linkUrl: String?) {
            var child = currentNode.firstChild
            while (child != null) {
                when (child) {
                    is Text -> {
                        runs.add(
                            StyledTextRun(
                                text = child.literal,
                                isBold = isBold,
                                isItalic = isItalic,
                                isStrikethrough = isStrikethrough,
                                isCode = isCode,
                                isHighlighted = isHighlighted,
                                isUnderline = isUnderline,
                                isSubscript = isSubscript,
                                isSuperscript = isSuperscript,
                                isKbd = isKbd,
                                linkUrl = linkUrl
                            )
                        )
                    }
                    is Code -> {
                        runs.add(
                            StyledTextRun(
                                text = child.literal,
                                isBold = isBold,
                                isItalic = isItalic,
                                isStrikethrough = isStrikethrough,
                                isCode = true,
                                isHighlighted = isHighlighted,
                                isUnderline = isUnderline,
                                isSubscript = isSubscript,
                                isSuperscript = isSuperscript,
                                isKbd = isKbd,
                                linkUrl = linkUrl
                            )
                        )
                    }
                    is SoftLineBreak -> {
                        runs.add(
                            StyledTextRun(
                                text = " ",
                                isBold = isBold,
                                isItalic = isItalic,
                                isStrikethrough = isStrikethrough,
                                isCode = isCode,
                                isHighlighted = isHighlighted,
                                isUnderline = isUnderline,
                                isSubscript = isSubscript,
                                isSuperscript = isSuperscript,
                                isKbd = isKbd,
                                linkUrl = linkUrl
                            )
                        )
                    }
                    is HardLineBreak -> {
                        runs.add(
                            StyledTextRun(
                                text = "\n",
                                isBold = isBold,
                                isItalic = isItalic,
                                isStrikethrough = isStrikethrough,
                                isCode = isCode,
                                isHighlighted = isHighlighted,
                                isUnderline = isUnderline,
                                isSubscript = isSubscript,
                                isSuperscript = isSuperscript,
                                isKbd = isKbd,
                                linkUrl = linkUrl
                            )
                        )
                    }
                    is StrongEmphasis -> {
                        visit(child, isBold = true, isItalic = isItalic, isStrikethrough = isStrikethrough, isCode = isCode, linkUrl = linkUrl)
                    }
                    is Emphasis -> {
                        visit(child, isBold = isBold, isItalic = true, isStrikethrough = isStrikethrough, isCode = isCode, linkUrl = linkUrl)
                    }
                    is Link -> {
                        visit(child, isBold = isBold, isItalic = isItalic, isStrikethrough = isStrikethrough, isCode = isCode, linkUrl = child.destination)
                    }
                    is Strikethrough -> {
                        visit(child, isBold = isBold, isItalic = isItalic, isStrikethrough = true, isCode = isCode, linkUrl = linkUrl)
                    }
                    is HtmlInline -> {
                        val literal = child.literal ?: ""
                        when {
                            literal.contains(Regex("(?i)<mark\\b")) -> isHighlighted = true
                            literal.contains(Regex("(?i)</mark>")) -> isHighlighted = false
                            literal.contains(Regex("(?i)<u\\b")) -> isUnderline = true
                            literal.contains(Regex("(?i)</u>")) -> isUnderline = false
                            literal.contains(Regex("(?i)<sub\\b")) -> isSubscript = true
                            literal.contains(Regex("(?i)</sub>")) -> isSubscript = false
                            literal.contains(Regex("(?i)<sup\\b")) -> isSuperscript = true
                            literal.contains(Regex("(?i)</sup>")) -> isSuperscript = false
                            literal.contains(Regex("(?i)<kbd\\b")) -> isKbd = true
                            literal.contains(Regex("(?i)</kbd>")) -> isKbd = false
                            literal.contains(Regex("(?i)<br\\s*/?>")) -> {
                                runs.add(
                                    StyledTextRun(
                                        text = "\n",
                                        isBold = isBold,
                                        isItalic = isItalic,
                                        isStrikethrough = isStrikethrough,
                                        isCode = isCode,
                                        isHighlighted = isHighlighted,
                                        isUnderline = isUnderline,
                                        isSubscript = isSubscript,
                                        isSuperscript = isSuperscript,
                                        isKbd = isKbd,
                                        linkUrl = linkUrl
                                    )
                                )
                            }
                        }
                    }
                    is Image -> {
                        val url = child.destination ?: ""
                        val altText = getTextContent(child)
                        runs.add(
                            StyledTextRun(
                                text = altText.ifEmpty { "[Image]" },
                                isBold = isBold,
                                isItalic = isItalic,
                                isStrikethrough = isStrikethrough,
                                isCode = isCode,
                                isHighlighted = isHighlighted,
                                isUnderline = isUnderline,
                                isSubscript = isSubscript,
                                isSuperscript = isSuperscript,
                                isKbd = isKbd,
                                linkUrl = linkUrl,
                                imageUrl = url
                            )
                        )
                    }
                    else -> {
                        visit(child, isBold, isItalic, isStrikethrough, isCode, linkUrl)
                    }
                }
                child = child.next
            }
        }

        visit(node, isBold = false, isItalic = false, isStrikethrough = false, isCode = false, linkUrl = null)
        return runs
    }

    private fun getTextContent(node: Node): String {
        val sb = StringBuilder()
        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                sb.append(text.literal)
            }
            override fun visit(softLineBreak: SoftLineBreak) {
                sb.append(" ")
            }
            override fun visit(hardLineBreak: HardLineBreak) {
                sb.append("\n")
            }
        })
        return sb.toString()
    }
}

