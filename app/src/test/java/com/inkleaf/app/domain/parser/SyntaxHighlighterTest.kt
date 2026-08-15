package com.inkleaf.app.domain.parser

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxHighlighterTest {

    @Test
    fun `highlights code keywords correctly`() {
        val code = "fun main() { val x = 1 }"
        val annotated = SyntaxHighlighter.highlight(code, "kotlin", "")

        // Verify that keywords like "fun" and "val" are styled
        // Keyword style is Color(0xFF38BDF8)
        val styles = annotated.spanStyles
        assertTrue("Expected keyword styles to be present", styles.isNotEmpty())

        val keywordStyles = styles.filter { it.item.color == Color(0xFF38BDF8) }
        assertEquals(2, keywordStyles.size) // "fun" and "val"
    }

    @Test
    fun `highlights strings and numbers`() {
        val code = "val str = \"hello\" \n val num = 42"
        val annotated = SyntaxHighlighter.highlight(code, "kotlin", "")

        val styles = annotated.spanStyles
        // String color is 0xFFFBBF24, Number color is 0xFFF472B6
        assertTrue(styles.any { it.item.color == Color(0xFFFBBF24) }) // "hello"
        assertTrue(styles.any { it.item.color == Color(0xFFF472B6) }) // 42
    }
}
