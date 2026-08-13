package com.inkleaf.app.domain.parser

import com.inkleaf.app.domain.model.*
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.ext.gfm.tables.TableBlock as GfmTableBlock
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TablesExtension

class MarkdownBlockParser {

    private val parser: Parser = Parser.builder()
        .extensions(listOf(TablesExtension.create()))
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
                    blocks.add(
                        HeadingBlock(
                            id = nextId(),
                            sourceRange = range,
                            level = node.level,
                            text = textContent
                        )
                    )
                }
                is Paragraph -> {
                    val textContent = getTextContent(node)
                    blocks.add(
                        ParagraphBlock(
                            id = nextId(),
                            sourceRange = range,
                            text = textContent
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
                    // Check for callout pattern: [!NOTE], [!TIP], etc.
                    val matchResult = Regex("""^\s*\[!(NOTE|TIP|WARNING|CAUTION)\](.*)""", RegexOption.DOT_MATCHES_ALL)
                        .find(textContent)
                    if (matchResult != null) {
                        val type = matchResult.groupValues[1]
                        val content = matchResult.groupValues[2].trim()
                        blocks.add(
                            CalloutBlock(
                                id = nextId(),
                                sourceRange = range,
                                type = type,
                                title = type,
                                content = content
                            )
                        )
                    } else {
                        // Fallback as code-styled BlockQuote
                        blocks.add(
                            CalloutBlock(
                                id = nextId(),
                                sourceRange = range,
                                type = "QUOTE",
                                title = null,
                                content = textContent
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
                else -> {
                    // Fallback block mapping
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

// Ensure HorizontalRuleBlock class is defined in BlockModel
