package com.inkleaf.app.domain.model

data class SourceRange(val startLine: Int, val endLine: Int)

sealed interface BlockModel {
    val id: String
    val sourceRange: SourceRange
}

data class ParagraphBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val text: String
) : BlockModel

data class HeadingBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val level: Int,
    val text: String
) : BlockModel

data class CodeBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val language: String?,
    val code: String
) : BlockModel

data class TableBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val headers: List<String>,
    val rows: List<List<String>>
) : BlockModel

data class CalloutBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val type: String, // TIP, NOTE, WARNING, CAUTION
    val title: String?,
    val content: String
) : BlockModel

data class MermaidBlock(
    override val id: String,
    override val sourceRange: SourceRange,
    val diagramSource: String
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

