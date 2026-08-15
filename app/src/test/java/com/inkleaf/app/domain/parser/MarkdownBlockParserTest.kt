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

        // The root level should contain exactly 2 list items ("Fruit" and "Vegetables")
        assertEquals(2, listItems.size)

        // "- Fruit"
        val fruitItem = listItems[0]
        assertEquals("Fruit", fruitItem.text)
        assertEquals(0, fruitItem.level)
        assertFalse(fruitItem.isOrdered)
        
        // Children of Fruit: "Apple" and "Orange"
        val fruitChildren = fruitItem.children.filterIsInstance<ListItemBlock>()
        assertEquals(2, fruitChildren.size)
        
        assertEquals("Apple", fruitChildren[0].text)
        assertEquals(1, fruitChildren[0].level)
        assertFalse(fruitChildren[0].isOrdered)

        assertEquals("Orange", fruitChildren[1].text)
        assertEquals(1, fruitChildren[1].level)
        assertFalse(fruitChildren[1].isOrdered)

        // "- Vegetables"
        val vegetableItem = listItems[1]
        assertEquals("Vegetables", vegetableItem.text)
        assertEquals(0, vegetableItem.level)
        assertFalse(vegetableItem.isOrdered)

        // Children of Vegetables: "Carrot" and "Potato"
        val vegetableChildren = vegetableItem.children.filterIsInstance<ListItemBlock>()
        assertEquals(2, vegetableChildren.size)

        assertEquals("Carrot", vegetableChildren[0].text)
        assertEquals(1, vegetableChildren[0].level)
        assertTrue(vegetableChildren[0].isOrdered)
        assertEquals(1, vegetableChildren[0].number)

        assertEquals("Potato", vegetableChildren[1].text)
        assertEquals(1, vegetableChildren[1].level)
        assertTrue(vegetableChildren[1].isOrdered)
        assertEquals(2, vegetableChildren[1].number)
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

    @Test
    fun `parses recursive blockquotes and callouts correctly`() {
        val markdown = """
            > [!NOTE]
            > Outer quote
            >
            > > Nested quote
        """.trimIndent()

        val blocks = parser.parseToBlocks(markdown)
        val callouts = blocks.filterIsInstance<com.inkleaf.app.domain.model.CalloutBlock>()

        assertEquals(1, callouts.size)
        val outerCallout = callouts[0]
        assertEquals("NOTE", outerCallout.type)
        assertEquals("NOTE", outerCallout.title)

        // Check children
        assertEquals(2, outerCallout.children.size)
        assertTrue(outerCallout.children[0] is com.inkleaf.app.domain.model.ParagraphBlock)
        assertEquals("Outer quote", (outerCallout.children[0] as com.inkleaf.app.domain.model.ParagraphBlock).text)

        assertTrue(outerCallout.children[1] is com.inkleaf.app.domain.model.CalloutBlock)
        val innerCallout = outerCallout.children[1] as com.inkleaf.app.domain.model.CalloutBlock
        assertEquals("QUOTE", innerCallout.type)
        assertEquals("Nested quote", (innerCallout.children[0] as com.inkleaf.app.domain.model.ParagraphBlock).text)
    }

    @Test
    fun `parses list items with nested block children correctly`() {
        val markdown = """
            - First item
              
              Continuation paragraph
              
              ```kotlin
              val x = 1
              ```
        """.trimIndent()

        val blocks = parser.parseToBlocks(markdown)
        val listItems = blocks.filterIsInstance<ListItemBlock>()

        assertEquals(1, listItems.size)
        val listItem = listItems[0]
        assertEquals("First item", listItem.text)

        // Children should contain the paragraph and code block
        assertEquals(2, listItem.children.size)
        assertTrue(listItem.children[0] is com.inkleaf.app.domain.model.ParagraphBlock)
        assertEquals("Continuation paragraph", (listItem.children[0] as com.inkleaf.app.domain.model.ParagraphBlock).text)

        assertTrue(listItem.children[1] is com.inkleaf.app.domain.model.CodeBlock)
        assertEquals("kotlin", (listItem.children[1] as com.inkleaf.app.domain.model.CodeBlock).language)
    }

    @Test
    fun `parses HTML inline mark tags as highlighted runs`() {
        val markdown = "This contains <mark>highlighted HTML</mark>."
        val blocks = parser.parseToBlocks(markdown)
        val paragraph = blocks.filterIsInstance<com.inkleaf.app.domain.model.ParagraphBlock>().first()

        val runs = paragraph.runs
        assertTrue(runs.any { it.text == "highlighted HTML" && it.isHighlighted })
    }

    @Test
    fun `parses standalone image references as ImageBlocks`() {
        val markdown = "![Sample image](https://placehold.co/640x360/png \"Image title\")"
        val blocks = parser.parseToBlocks(markdown)

        val images = blocks.filterIsInstance<com.inkleaf.app.domain.model.ImageBlock>()
        assertEquals(1, images.size)
        assertEquals("https://placehold.co/640x360/png", images[0].url)
        assertEquals("Sample image", images[0].altText)
        assertEquals("Image title", images[0].title)
    }

    @Test
    fun `parses full rendering test suite without crashing`() {
        val file = java.io.File("C:\\Luminary\\Projects\\inkleaf\\docs\\sample\\Inkleaf_Markdown_Rendering_Test_Suite.md")
        val content = file.readText()
        val blocks = parser.parseToBlocks(content)
        assertTrue(blocks.isNotEmpty())
    }
}
