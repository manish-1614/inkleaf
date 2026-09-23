package com.inkleaf.app.ui.reader

import com.inkleaf.app.domain.model.HeadingBlock
import com.inkleaf.app.domain.model.ParagraphBlock
import com.inkleaf.app.domain.model.SourceRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `stress test indexing and late heading jumps with 5000 blocks and 200 headings`() {
        val totalBlocks = 5000
        val headingInterval = 25
        val blocks = (0 until totalBlocks).map { i ->
            if (i % headingInterval == 0) {
                val headingNum = i / headingInterval
                HeadingBlock(
                    id = "heading-$headingNum",
                    sourceRange = SourceRange(i * 10, i * 10 + 9),
                    level = (headingNum % 6) + 1,
                    text = "Heading $headingNum"
                )
            } else {
                ParagraphBlock(
                    id = "para-$i",
                    sourceRange = SourceRange(i * 10, i * 10 + 9),
                    text = "Paragraph body text for block $i"
                )
            }
        }

        val headingMap = headingIndexById(blocks)
        assertEquals(200, headingMap.size)

        // Validate late heading lookups (near the end of the 5,000 blocks)
        val h195Index = headingMap["heading-195"]
        val h198Index = headingMap["heading-198"]
        val h199Index = headingMap["heading-199"]

        assertEquals(195 * headingInterval, h195Index)
        assertEquals(198 * headingInterval, h198Index)
        assertEquals(199 * headingInterval, h199Index)
        assertEquals(4975, h199Index)
    }

    @Test
    fun `clampScrollTarget safely bounds targets in large documents`() {
        val totalBlocks = 5000

        assertEquals(0, clampScrollTarget(0, totalBlocks))
        assertEquals(4999, clampScrollTarget(4999, totalBlocks))
        assertEquals(4999, clampScrollTarget(5000, totalBlocks))
        assertEquals(4999, clampScrollTarget(99999, totalBlocks))
        assertEquals(0, clampScrollTarget(-10, totalBlocks))
        assertEquals(0, clampScrollTarget(100, 0))
    }

    @Test
    fun `shouldUseInstantScroll triggers when jump delta exceeds threshold`() {
        assertTrue(shouldUseInstantScroll(targetIndex = 4975, currentIndex = 0))
        assertTrue(shouldUseInstantScroll(targetIndex = 0, currentIndex = 4975))
        assertTrue(shouldUseInstantScroll(targetIndex = 100, currentIndex = 69)) // delta 31 > 30

        assertFalse(shouldUseInstantScroll(targetIndex = 100, currentIndex = 70)) // delta 30
        assertFalse(shouldUseInstantScroll(targetIndex = 100, currentIndex = 100)) // delta 0
        assertFalse(shouldUseInstantScroll(targetIndex = 10, currentIndex = 25)) // delta 15
    }
}

