package com.inkleaf.app.domain.parser

import com.inkleaf.app.domain.model.ListItemBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownBlockParserTest {

    private val parser = MarkdownBlockParser()

    @Test
    fun `parses simple unordered lists`() {
        val markdown = """
            - Apple
            - Banana
            - Cherry
        """.trimIndent()

        val blocks = parser.parseToBlocks(markdown)
        val listItems = blocks.filterIsInstance<ListItemBlock>()

        assertEquals(3, listItems.size)
        assertEquals("Apple", listItems[0].text)
        assertEquals(0, listItems[0].level)
        assertFalse(listItems[0].isOrdered)
        assertFalse(listItems[0].isTask)

        assertEquals("Banana", listItems[1].text)
        assertEquals("Cherry", listItems[2].text)
    }

    @Test
    fun `parses nested and mixed list levels`() {
        val markdown = """
            - Fruit
              - Apple
              - Orange
            - Vegetables
              1. Carrot
              2. Potato
        """.trimIndent()

        val blocks = parser.parseToBlocks(markdown)
        val listItems = blocks.filterIsInstance<ListItemBlock>()

        assertEquals(6, listItems.size)

        // "- Fruit"
        assertEquals("Fruit", listItems[0].text)
        assertEquals(0, listItems[0].level)
        assertFalse(listItems[0].isOrdered)

        // "  - Apple"
        assertEquals("Apple", listItems[1].text)
        assertEquals(1, listItems[1].level)
        assertFalse(listItems[1].isOrdered)

        // "  - Orange"
        assertEquals("Orange", listItems[2].text)
        assertEquals(1, listItems[2].level)

        // "- Vegetables"
        assertEquals("Vegetables", listItems[3].text)
        assertEquals(0, listItems[3].level)

        // "  1. Carrot"
        assertEquals("Carrot", listItems[4].text)
        assertEquals(1, listItems[4].level)
        assertTrue(listItems[4].isOrdered)
        assertEquals(1, listItems[4].number)

        // "  2. Potato"
        assertEquals("Potato", listItems[5].text)
        assertEquals(1, listItems[5].level)
        assertTrue(listItems[5].isOrdered)
        assertEquals(2, listItems[5].number)
    }

    @Test
    fun `parses GFM task lists with checkbox states`() {
        val markdown = """
            - [ ] Todo Item
            - [x] Done Item
            - [X] Also Done Item
        """.trimIndent()

        val blocks = parser.parseToBlocks(markdown)
        val listItems = blocks.filterIsInstance<ListItemBlock>()

        assertEquals(3, listItems.size)

        assertTrue(listItems[0].isTask)
        assertFalse(listItems[0].isChecked)
        assertEquals("Todo Item", listItems[0].text)

        assertTrue(listItems[1].isTask)
        assertTrue(listItems[1].isChecked)
        assertEquals("Done Item", listItems[1].text)

        assertTrue(listItems[2].isTask)
        assertTrue(listItems[2].isChecked)
        assertEquals("Also Done Item", listItems[2].text)
    }

    @Test
    fun `parses inline formatting runs correctly`() {
        val markdown = "This is **bold** and *italic* with `code` and ~~strike~~ and [link](url)."
        val blocks = parser.parseToBlocks(markdown)
        val paragraph = blocks.filterIsInstance<com.inkleaf.app.domain.model.ParagraphBlock>().first()

        val runs = paragraph.runs
        assertTrue(runs.any { it.text == "bold" && it.isBold })
        assertTrue(runs.any { it.text == "italic" && it.isItalic })
        assertTrue(runs.any { it.text == "code" && it.isCode })
        assertTrue(runs.any { it.text == "strike" && it.isStrikethrough })
        assertTrue(runs.any { it.text == "link" && it.linkUrl == "url" })
    }
}
