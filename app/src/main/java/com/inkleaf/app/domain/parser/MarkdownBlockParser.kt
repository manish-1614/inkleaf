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

class MarkdownBlockParser {

    private val parser: Parser = Parser.builder()
        .extensions(listOf(TablesExtension.create(), StrikethroughExtension.create()))
        .build()

    fun parseToBlocks(markdownSource: String): List<BlockModel> {
        val document = parser.parse(markdownSource)
        val blocks = mutableListOf<BlockModel>()
        var blockIdCounter = 0

        fun nextId() = "block_${blockIdCounter++}"

        var node = document.firstChild
        while (node != null) {
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
                    val textContent = getTextContent(node)
                    val runs = parseRuns(node)
                    val matchResult = Regex("""^\s*\[!(NOTE|TIP|WARNING|CAUTION)\](.*)""", RegexOption.DOT_MATCHES_ALL)
                        .find(textContent)
                    if (matchResult != null) {
                        val type = matchResult.groupValues[1]
                        val content = matchResult.groupValues[2].trim()

                        var cleanedRuns = runs
                        if (runs.isNotEmpty()) {
                            val calloutRegex = Regex("""^\s*\[!(?:NOTE|TIP|WARNING|CAUTION)\]\s*(.*)""", RegexOption.DOT_MATCHES_ALL)
                            val firstRunMatch = calloutRegex.find(runs[0].text)
                            if (firstRunMatch != null) {
                                val mutableRuns = runs.toMutableList()
                                mutableRuns[0] = runs[0].copy(text = firstRunMatch.groupValues[1])
                                cleanedRuns = mutableRuns
                            }
                        }

                        blocks.add(
                            CalloutBlock(
                                id = nextId(),
                                sourceRange = range,
                                type = type,
                                title = type,
                                content = content,
                                runs = cleanedRuns
                            )
                        )
                    } else {
                        blocks.add(
                            CalloutBlock(
                                id = nextId(),
                                sourceRange = range,
                                type = "QUOTE",
                                title = null,
                                content = textContent,
                                runs = runs
                            )
                        )
                    }
                }
                is GfmTableBlock -> {
                    val headers = mutableListOf<String>()
                    val rows = mutableListOf<List<String>>()

                    var tablePart = node.firstChild
                    while (tablePart != null) {
                        if (tablePart is TableHead) {
                            var row = tablePart.firstChild
                            while (row != null) {
                                if (row is TableRow) {
                                    var cell = row.firstChild
                                    while (cell != null) {
                                        if (cell is TableCell) {
                                            headers.add(getTextContent(cell))
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
                                    val rowData = mutableListOf<String>()
                                    var cell = row.firstChild
                                    while (cell != null) {
                                        if (cell is TableCell) {
                                            rowData.add(getTextContent(cell))
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
                    parseList(node, 0, ::nextId, blocks)
                }
                is OrderedList -> {
                    parseList(node, 0, ::nextId, blocks)
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
            node = node.next
        }
        return blocks
    }

    private fun parseList(
        listNode: ListBlock,
        level: Int,
        nextId: () -> String,
        blocks: MutableList<BlockModel>
    ) {
        val isOrdered = listNode is OrderedList
        var itemNumber = if (isOrdered) (listNode as OrderedList).startNumber else 1

        var listItem = listNode.firstChild
        while (listItem != null) {
            if (listItem is ListItem) {
                val textContent = getListItemTextContent(listItem)
                val rawRuns = getListItemRuns(listItem)

                var isTask = false
                var isChecked = false
                var cleanedText = textContent
                var cleanedRuns = rawRuns

                if (rawRuns.isNotEmpty()) {
                    val firstRunText = rawRuns[0].text
                    val taskRegex = Regex("""^\[([ xX])\]\s*(.*)""", RegexOption.DOT_MATCHES_ALL)
                    val match = taskRegex.find(firstRunText)
                    if (match != null) {
                        isTask = true
                        isChecked = match.groupValues[1].lowercase() == "x"
                        val remainingText = match.groupValues[2]

                        val mutableRuns = rawRuns.toMutableList()
                        mutableRuns[0] = rawRuns[0].copy(text = remainingText)
                        cleanedRuns = mutableRuns

                        // Update plain text content too
                        val plainMatch = taskRegex.find(textContent)
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
                        runs = cleanedRuns
                    )
                )

                if (isOrdered) {
                    itemNumber++
                }

                var child = listItem.firstChild
                while (child != null) {
                    if (child is ListBlock) {
                        parseList(child, level + 1, nextId, blocks)
                    }
                    child = child.next
                }
            }
            listItem = listItem.next
        }
    }

    private fun parseRuns(node: Node): List<StyledTextRun> {
        val runs = mutableListOf<StyledTextRun>()

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

    private fun getListItemRuns(listItem: ListItem): List<StyledTextRun> {
        val runs = mutableListOf<StyledTextRun>()
        var child = listItem.firstChild
        while (child != null) {
            if (child !is ListBlock) {
                runs.addAll(parseRuns(child))
            }
            child = child.next
        }
        return runs
    }

    private fun getListItemTextContent(listItem: ListItem): String {
        val sb = StringBuilder()
        var child = listItem.firstChild
        while (child != null) {
            if (child !is ListBlock) {
                val content = getTextContent(child).trim()
                if (content.isNotEmpty()) {
                    if (sb.isNotEmpty()) sb.append(" ")
                    sb.append(content)
                }
            }
            child = child.next
        }
        return sb.toString()
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
