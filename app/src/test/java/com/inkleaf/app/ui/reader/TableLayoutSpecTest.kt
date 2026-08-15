package com.inkleaf.app.ui.reader

import com.inkleaf.app.domain.model.TableCellModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableLayoutSpecTest {
    @Test
    fun `column width is shared across header and every data row`() {
        val widths = tableColumnWidths(
            headers = listOf("#", "What it captures").map { TableCellModel(it) },
            rows = listOf(
                listOf("1", "Mood, density, design").map { TableCellModel(it) },
                listOf("2", "Semantic name + hex + function").map { TableCellModel(it) }
            )
        )

        assertEquals(2, widths.size)
        assertTrue(widths[1] > widths[0])
        assertTrue(widths[1] >= 24f + ("Semantic name + hex + function".length * 8f))
    }

    @Test
    fun `table columns include missing cells as empty values`() {
        val widths = tableColumnWidths(
            headers = listOf("Name", "Description").map { TableCellModel(it) },
            rows = listOf(listOf("Only one cell").map { TableCellModel(it) })
        )

        assertEquals(2, widths.size)
        assertTrue(widths.all { it > 0f })
    }
}
