package com.inkleaf.app.ui.reader

import com.inkleaf.app.domain.model.TableCellModel

private const val TABLE_HORIZONTAL_PADDING_DP = 24f
private const val TABLE_CHARACTER_WIDTH_DP = 8f
private const val TABLE_MIN_COLUMN_WIDTH_DP = 48f
private const val TABLE_MAX_COLUMN_WIDTH_DP = 360f

fun tableColumnWidths(headers: List<TableCellModel>, rows: List<List<TableCellModel>>): List<Float> {
    val columnCount = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
    if (columnCount == 0) return emptyList()

    return List(columnCount) { columnIndex ->
        val longestCell = sequenceOf(headers.getOrNull(columnIndex)?.text.orEmpty())
            .plus(rows.asSequence().map { it.getOrNull(columnIndex)?.text.orEmpty() })
            .flatMap { it.lineSequence() }
            .maxOfOrNull { it.length }
            ?: 0

        (TABLE_HORIZONTAL_PADDING_DP + longestCell * TABLE_CHARACTER_WIDTH_DP)
            .coerceIn(TABLE_MIN_COLUMN_WIDTH_DP, TABLE_MAX_COLUMN_WIDTH_DP)
    }
}

fun normalizedTableRow(row: List<TableCellModel>, columnCount: Int): List<TableCellModel> =
    List(columnCount) { row.getOrNull(it) ?: TableCellModel("") }
