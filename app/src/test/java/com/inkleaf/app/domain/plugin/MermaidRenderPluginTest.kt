package com.inkleaf.app.domain.plugin

import com.inkleaf.app.data.diagram.DiagramRepository
import com.inkleaf.app.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MermaidRenderPluginTest {

    private lateinit var plugin: MermaidRenderPlugin
    private lateinit var repository: DiagramRepository

    @Before
    fun setUp() {
        repository = DiagramRepository.getInstance()
        repository.clear()
        plugin = MermaidRenderPlugin(repository)
    }

    @Test
    fun `extractPreviewLabel extracts YAML frontmatter title`() {
        val source = """
            ---
            title: System Architecture v2
            ---
            flowchart TD
                A --> B
        """.trimIndent()

        val label = plugin.extractPreviewLabel(source)
        assertEquals("System Architecture v2", label)
    }

    @Test
    fun `extractPreviewLabel extracts title directive`() {
        val source = """
            title Component Pipeline
            flowchart LR
                A --> B
        """.trimIndent()

        val label = plugin.extractPreviewLabel(source)
        assertEquals("Component Pipeline", label)
    }

    @Test
    fun `extractPreviewLabel extracts first node square bracket label`() {
        val source = """
            flowchart TD
                Start[Begin Ingestion] --> Parse[Parse Document]
        """.trimIndent()

        val label = plugin.extractPreviewLabel(source)
        assertEquals("Begin Ingestion", label)
    }

    @Test
    fun `extractPreviewLabel extracts first node paren label`() {
        val source = """
            graph TD
                id1("Rounded Node") --> id2
        """.trimIndent()

        val label = plugin.extractPreviewLabel(source)
        assertEquals("Rounded Node", label)
    }

    @Test
    fun `extractPreviewLabel extracts sequence diagram message`() {
        val source = """
            sequenceDiagram
                Client->>Server: Request Document
                Server-->>Client: 200 OK
        """.trimIndent()

        val label = plugin.extractPreviewLabel(source)
        assertEquals("Request Document", label)
    }

    @Test
    fun `extractPreviewLabel extracts class diagram class name`() {
        val source = """
            classDiagram
                class OrderManager
        """.trimIndent()

        val label = plugin.extractPreviewLabel(source)
        assertEquals("OrderManager", label)
    }

    @Test
    fun `extractPreviewLabel returns null when no title or node label is extractable`() {
        val source = """
            flowchart TD
                A --> B
                B --> C
        """.trimIndent()

        val label = plugin.extractPreviewLabel(source)
        assertNull(label)
    }

    @Test
    fun `createPlaceholderBlock creates DiagramPlaceholderBlock and stores source in repository`() {
        val source = """
            flowchart TD
                Init[Initialize Kernel] --> Run
        """.trimIndent()

        val placeholder = plugin.createPlaceholderBlock(
            diagramSource = source,
            sourceRange = SourceRange(1, 3),
            position = 2,
            blockId = "block_custom_id"
        )

        assertEquals("block_custom_id", placeholder.id)
        assertEquals("mermaid", placeholder.language)
        assertEquals("Initialize Kernel", placeholder.previewLabel)
        assertEquals(SourceRange(1, 3), placeholder.sourceRange)

        // Raw source should be stored in repository by diagramId
        val stored = repository.getDiagram(placeholder.diagramId)
        assertEquals(source, stored)
    }

    @Test
    fun `render stores source and returns DiagramPlaceholderArtifact`() = runBlocking {
        val source = "flowchart LR\n  A[Login] --> B[Dashboard]"
        val mermaidBlock = MermaidBlock(
            id = "block_10",
            sourceRange = SourceRange(5, 7),
            diagramSource = source
        )

        val input = PluginInput(
            block = mermaidBlock,
            sourceText = source,
            themeId = "dark",
            containerWidthDp = 400
        )

        val artifact = plugin.render(input)

        assertTrue(artifact is DiagramPlaceholderArtifact)
        val placeholderArtifact = artifact as DiagramPlaceholderArtifact
        assertEquals("mermaid_webview_renderer", placeholderArtifact.rendererId)
        assertEquals("Login", placeholderArtifact.placeholderBlock.previewLabel)
        assertEquals("mermaid", placeholderArtifact.placeholderBlock.language)

        val stored = repository.getDiagram(placeholderArtifact.diagramId)
        assertEquals(source, stored)
    }

    @Test
    fun `detect recognizes MermaidBlock and DiagramPlaceholderBlock`() = runBlocking {
        val mermaidBlock = MermaidBlock("id1", SourceRange(0, 0), "flowchart TD\n A")
        val placeholderBlock = DiagramPlaceholderBlock("id2", SourceRange(0, 0), "diag1", "mermaid", "Preview")
        val otherBlock = CodeBlock("id3", SourceRange(0, 0), "kotlin", "val x = 1")

        val detectMermaid = plugin.detect(PluginInput(mermaidBlock, "", "", 0))
        val detectPlaceholder = plugin.detect(PluginInput(placeholderBlock, "", "", 0))
        val detectOther = plugin.detect(PluginInput(otherBlock, "", "", 0))

        assertTrue(detectMermaid.canHandle)
        assertTrue(detectPlaceholder.canHandle)
        assertFalse(detectOther.canHandle)
    }
}
