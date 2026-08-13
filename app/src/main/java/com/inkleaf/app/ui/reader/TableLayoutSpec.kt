package com.inkleaf.app.ui.reader

private const val TABLE_HORIZONTAL_PADDING_DP = 24f
private const val TABLE_CHARACTER_WIDTH_DP = 8f
private const val TABLE_MIN_COLUMN_WIDTH_DP = 72f
private const val TABLE_MAX_COLUMN_WIDTH_DP = 360f

fun tableColumnWidths(headers: List<String>, rows: List<List<String>>): List<Float> {
    val columnCount = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
    if (columnCount == 0) return emptyList()

    return List(columnCount) { columnIndex ->
        val longestCell = sequenceOf(headers.getOrNull(columnIndex).orEmpty())
            .plus(rows.asSequence().map { it.getOrNull(columnIndex).orEmpty() })
            .flatMap { it.lineSequence() }
            .maxOfOrNull { it.length }
            ?: 0

        (TABLE_HORIZONTAL_PADDING_DP + longestCell * TABLE_CHARACTER_WIDTH_DP)
            .coerceIn(TABLE_MIN_COLUMN_WIDTH_DP, TABLE_MAX_COLUMN_WIDTH_DP)
    }
}

fun normalizedTableRow(row: List<String>, columnCount: Int): List<String> =
    List(columnCount) { row.getOrNull(it).orEmpty() }
