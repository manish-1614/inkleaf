package com.inkleaf.app.domain.model

data class SourceRange(val startLine: Int, val endLine: Int)

sealed interface BlockModel {
    val id: String
    val sourceRange: SourceRange
}

data class StyledTextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isCode: Boolean = false,
    val isHighlighted: Boolean = false,
    val isUnderline: Boolean = false,
    val isSubscript: Boolean = false,
    val isSuperscript: Boolean = false,
    val isKbd: Boolean = false,
    val linkUrl: String? = null,
    val imageUrl: String? = null
)

data class ParagraphBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val text: String,
    val runs: List<StyledTextRun> = emptyList()
) : BlockModel

data class HeadingBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val level: Int,
    val text: String,
    val runs: List<StyledTextRun> = emptyList()
) : BlockModel

data class CodeBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val language: String?,
    val code: String
) : BlockModel

enum class TableCellAlignment {
    LEFT, CENTER, RIGHT
}

data class TableCellModel(
    val text: String,
    val runs: List<StyledTextRun> = emptyList(),
    val alignment: TableCellAlignment = TableCellAlignment.LEFT
)

data class TableBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val headers: List<TableCellModel>,
    val rows: List<List<TableCellModel>>
) : BlockModel

data class CalloutBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val type: String, // NOTE, TIP, WARNING, CAUTION, QUOTE
    val title: String?,
    val children: List<BlockModel>
) : BlockModel

data class MermaidBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val diagramSource: String
) : BlockModel

data class DiagramPlaceholderBlock(
    override val id: String,
    override val sourceRange: SourceRange = SourceRange(0, 0),
    val diagramId: String,
    val language: String = "mermaid",
    val previewLabel: String? = null
) : BlockModel

data class MathBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val latex: String,
    val isInline: Boolean
) : BlockModel

data class SvgBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val svgContent: String
) : BlockModel

data class ImageBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val url: String,
    val altText: String?,
    val title: String?
) : BlockModel

data class RawFallbackBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val rawText: String,
    val errorMessage: String? = null
) : BlockModel

data class HorizontalRuleBlock(
    override val id: String,
    override val sourceRange: SourceRange
) : BlockModel

data class ListItemBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val text: String,
    val level: Int, // Nesting depth (0 = top level)
    val isOrdered: Boolean,
    val number: Int?, // Item number if ordered list (1, 2, ...)
    val isTask: Boolean,
    val isChecked: Boolean,
    val runs: List<StyledTextRun> = emptyList(),
    val children: List<BlockModel> = emptyList()
) : BlockModel



