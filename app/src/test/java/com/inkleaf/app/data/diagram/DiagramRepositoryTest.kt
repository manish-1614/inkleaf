package com.inkleaf.app.data.diagram

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DiagramRepositoryTest {

    private lateinit var repository: DiagramRepository

    @Before
    fun setUp() {
        repository = DiagramRepository.getInstance()
        repository.clear()
    }

    @Test
    fun `store and retrieve diagram successfully`() {
        val diagramSource = "flowchart TD\n  A --> B"
        val diagramId = repository.computeDiagramId(diagramSource, 0)

        repository.storeDiagram(diagramId, diagramSource)

        assertTrue(repository.containsDiagram(diagramId))
        assertEquals(diagramSource, repository.getDiagram(diagramId))
        assertEquals(1, repository.size)
    }

    @Test
    fun `computeDiagramId is stable across identical inputs and positions`() {
        val source = "sequenceDiagram\n  Alice->>Bob: Hi"
        val id1 = repository.computeDiagramId(source, 5)
        val id2 = repository.computeDiagramId(source, 5)
        val idDifferentPos = repository.computeDiagramId(source, 6)

        assertEquals(id1, id2)
        assertNotEquals(id1, idDifferentPos)
    }

    @Test
    fun `computeDiagramId changes when source content changes`() {
        val id1 = repository.computeDiagramId("flowchart TD\n  A --> B", 0)
        val id2 = repository.computeDiagramId("flowchart TD\n  A --> C", 0)

        assertNotEquals(id1, id2)
    }

    @Test
    fun `resetForDocument clears diagrams when document fingerprint changes`() {
        repository.resetForDocument("fingerprint_doc_A")
        val id1 = repository.computeDiagramId("diagram 1", 0)
        repository.storeDiagram(id1, "diagram 1")
        assertEquals(1, repository.size)

        // Switching to a new document fingerprint clears previous diagrams
        repository.resetForDocument("fingerprint_doc_B")
        assertEquals(0, repository.size)
        assertNull(repository.getDiagram(id1))
    }

    @Test
    fun `resetForDocument retains diagrams when document fingerprint is identical`() {
        repository.resetForDocument("fingerprint_doc_A")
        val id1 = repository.computeDiagramId("diagram 1", 0)
        repository.storeDiagram(id1, "diagram 1")

        // Same fingerprint does not clear
        repository.resetForDocument("fingerprint_doc_A")
        assertEquals(1, repository.size)
        assertNotNull(repository.getDiagram(id1))
    }

    @Test
    fun `clear empties all stored diagrams`() {
        repository.storeDiagram("id1", "content 1")
        repository.storeDiagram("id2", "content 2")
        assertEquals(2, repository.size)

        repository.clear()
        assertEquals(0, repository.size)
        assertNull(repository.getDiagram("id1"))
        assertNull(repository.getDiagram("id2"))
    }
}
