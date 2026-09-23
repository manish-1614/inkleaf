package com.inkleaf.app.domain.plugin

import com.inkleaf.app.data.diagram.DiagramRepository
import com.inkleaf.app.domain.model.*

class MermaidRenderPlugin(
    private val diagramRepository: DiagramRepository = DiagramRepository.getInstance()
) : RenderPlugin {

    override val id: String = "mermaid_webview_renderer"
    override val version: String = "10.9.0"
    override val languageTags: Set<String> = setOf("mermaid")
    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability.OFFLINE_ONLY,
        PluginCapability.HTML_OUTPUT
    )

    /**
     * Extracts a human-readable title or the first node label from the Mermaid diagram source.
     * Returns null if no title or node label is easily extractable, allowing the UI to fall
     * back to a generic "Diagram" label.
     */
    fun extractPreviewLabel(diagramSource: String): String? {
        val lines = diagramSource.lines()

        // 1. Check for YAML frontmatter title: --- \n title: XYZ \n ---
        var inFrontmatter = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "---") {
                if (inFrontmatter) break else { inFrontmatter = true; continue }
            }
            if (inFrontmatter && trimmed.startsWith("title:", ignoreCase = true)) {
                val title = trimmed.substringAfter(":").trim().trim('"', '\'')
                if (title.isNotEmpty()) return title.take(40)
            }
        }

        // 2. Check for title directive: "title XYZ" or "title: XYZ" or "accTitle: XYZ"
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("title ", ignoreCase = true) || trimmed.startsWith("title:", ignoreCase = true)) {
                val title = trimmed.substring(5).trim().trim(':', ' ', '"', '\'')
                if (title.isNotEmpty()) return title.take(40)
            }
            if (trimmed.startsWith("accTitle:", ignoreCase = true)) {
                val title = trimmed.substringAfter(":").trim().trim('"', '\'')
                if (title.isNotEmpty()) return title.take(40)
            }
        }

        // 3. Check for first node definition with a label: id[Label], id(Label), id{Label}, id[(Label)], id["Label"]
        val nodeRegex = Regex("""\b[A-Za-z0-9_-]+(?:\[(?:\"|\')?(.*?)(?:\"|\')?\]|\((?:\"|\')?(.*?)(?:\"|\')?\)|\{(?:\"|\')?(.*?)(?:\"|\')?\})""")
        for (line in lines) {
            val trimmed = line.trim()
            // Skip diagram type declarations and comment lines
            if (trimmed.startsWith("flowchart", ignoreCase = true) ||
                trimmed.startsWith("graph", ignoreCase = true) ||
                trimmed.startsWith("sequenceDiagram", ignoreCase = true) ||
                trimmed.startsWith("classDiagram", ignoreCase = true) ||
                trimmed.startsWith("erDiagram", ignoreCase = true) ||
                trimmed.startsWith("stateDiagram", ignoreCase = true) ||
                trimmed.startsWith("subgraph", ignoreCase = true) ||
                trimmed.startsWith("%%")
            ) {
                continue
            }

            val match = nodeRegex.find(trimmed)
            if (match != null) {
                val label = (match.groups[1]?.value ?: match.groups[2]?.value ?: match.groups[3]?.value)
                    ?.trim()?.trim('"', '\'')
                if (!label.isNullOrBlank() && !label.startsWith("http", ignoreCase = true)) {
                    return label.take(40)
                }
            }
        }

        // 4. Check for sequence diagram message: Alice->>Bob: Message
        val seqMsgRegex = Regex("""[A-Za-z0-9_-]+\s*(?:->>|-->>|->|-->)\s*[A-Za-z0-9_-]+:\s*(.+)""")
        for (line in lines) {
            val trimmed = line.trim()
            val match = seqMsgRegex.find(trimmed)
            if (match != null) {
                val msg = match.groupValues[1].trim().trim('"', '\'')
                if (msg.isNotBlank()) {
                    return msg.take(40)
                }
            }
        }

        // 5. Check for class declaration: class Animal
        val classRegex = Regex("""class\s+([A-Za-z0-9_-]+)""")
        for (line in lines) {
            val match = classRegex.find(line.trim())
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.isNotEmpty()) return name.take(40)
            }
        }

        return null
    }

    /**
     * Stores raw diagram source in in-memory repository and returns a DiagramPlaceholderBlock.
     */
    fun createPlaceholderBlock(
        diagramSource: String,
        sourceRange: SourceRange = SourceRange(0, 0),
        position: Int = 0,
        blockId: String? = null
    ): DiagramPlaceholderBlock {
        val diagramId = diagramRepository.computeDiagramId(diagramSource, position)
        diagramRepository.storeDiagram(diagramId, diagramSource)
        val previewLabel = extractPreviewLabel(diagramSource)
        return DiagramPlaceholderBlock(
            id = blockId ?: "diagram_$diagramId",
            sourceRange = sourceRange,
            diagramId = diagramId,
            language = "mermaid",
            previewLabel = previewLabel
        )
    }

    override suspend fun detect(input: PluginInput): DetectionResult {
        val canHandle = input.block is MermaidBlock || input.block is DiagramPlaceholderBlock
        return DetectionResult(canHandle = canHandle, confidence = if (canHandle) 1.0f else 0.0f)
    }

    /**
     * Instead of producing a heavy inline HTML artifact that instantiates a WebView,
     * stores the diagram source into the repository and emits a DiagramPlaceholderBlock
     * wrapped in a DiagramPlaceholderArtifact.
     */
    override suspend fun render(input: PluginInput): RenderArtifact {
        val diagramSource = when (val block = input.block) {
            is MermaidBlock -> block.diagramSource
            is DiagramPlaceholderBlock -> diagramRepository.getDiagram(block.diagramId) ?: input.sourceText
            else -> input.sourceText
        }

        val placeholder = createPlaceholderBlock(
            diagramSource = diagramSource,
            sourceRange = input.block.sourceRange,
            position = 0,
            blockId = input.block.id
        )

        return DiagramPlaceholderArtifact(
            rendererId = id,
            rendererVersion = version,
            diagramId = placeholder.diagramId,
            placeholderBlock = placeholder
        )
    }
}
