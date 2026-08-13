package com.inkleaf.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeBlockHeaderLayoutTest {
    @Test
    fun `recognized language shows badge and keeps copy anchored right`() {
        val layout = codeBlockHeaderLayout("bash")

        assertTrue(layout.showLanguageBadge)
        assertEquals(CodeBlockCopyAnchor.End, layout.copyAnchor)
    }

    @Test
    fun `unknown language hides badge and keeps copy anchored right`() {
        val layout = codeBlockHeaderLayout(null)

        assertFalse(layout.showLanguageBadge)
        assertEquals(CodeBlockCopyAnchor.End, layout.copyAnchor)
    }
}
