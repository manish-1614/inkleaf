package com.inkleaf.app.ui.reader

import com.inkleaf.app.domain.model.HeadingBlock
import com.inkleaf.app.domain.model.ParagraphBlock
import com.inkleaf.app.domain.model.SourceRange
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderNavigationTest {
    @Test
    fun `heading index map targets the selected heading even when its level repeats`() {
        val blocks = listOf(
            HeadingBlock("h1", SourceRange(0, 0), 2, "First"),
            ParagraphBlock("p1", SourceRange(0, 0), "Body"),
            HeadingBlock("h2", SourceRange(0, 0), 2, "Second")
        )

        assertEquals(2, headingIndexById(blocks)["h2"])
    }
}
