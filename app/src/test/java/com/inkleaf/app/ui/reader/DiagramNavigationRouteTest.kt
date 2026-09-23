package com.inkleaf.app.ui.reader

import com.inkleaf.app.data.diagram.DiagramRepository
import org.junit.Assert.*
import org.junit.Test

class DiagramNavigationRouteTest {

    @Test
    fun `diagram route pattern conforms to diagram slash diagramId`() {
        val diagramId = "abc123def4567890"
        val route = "diagram/$diagramId"

        assertTrue(route.startsWith("diagram/"))
        assertEquals("abc123def4567890", route.removePrefix("diagram/"))
    }

    @Test
    fun `navigation argument never contains raw diagram text or newlines`() {
        val rawSource = """
            flowchart TD
                A[Start Node] --> B[Complex Processing With Lots of Data]
                B --> C{Decision Matrix}
        """.trimIndent()

        val diagramId = DiagramRepository.getInstance().computeDiagramId(rawSource, 1)
        val route = "diagram/$diagramId"

        assertFalse(route.contains("\n"))
        assertFalse(route.contains("flowchart"))
        assertFalse(route.contains("Start Node"))
        assertEquals(40, route.length) // "diagram/" (8) + 32 hex chars (32) = 40 chars
    }
}
