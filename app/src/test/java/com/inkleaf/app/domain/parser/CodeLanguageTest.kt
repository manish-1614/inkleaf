package com.inkleaf.app.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeLanguageTest {
    @Test
    fun `recognizes normalized language info strings`() {
        assertEquals("bash", normalizeCodeLanguage(" bash extra"))
        assertEquals("kotlin", normalizeCodeLanguage("KOTLIN"))
    }

    @Test
    fun `hides labels for unknown and non-language syntax`() {
        assertNull(normalizeCodeLanguage(null))
        assertNull(normalizeCodeLanguage(""))
        assertNull(normalizeCodeLanguage("plain text"))
        assertNull(normalizeCodeLanguage("mermaid"))
        assertNull(normalizeCodeLanguage("diagram"))
        assertNull(normalizeCodeLanguage("custom-dsl"))
    }
}
