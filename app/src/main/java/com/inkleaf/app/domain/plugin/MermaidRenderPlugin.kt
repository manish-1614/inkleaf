package com.inkleaf.app.domain.plugin

import com.inkleaf.app.domain.model.*

class MermaidRenderPlugin : RenderPlugin {

    override val id: String = "mermaid_webview_renderer"
    override val version: String = "10.9.0"
    override val languageTags: Set<String> = setOf("mermaid")
    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability.OFFLINE_ONLY,
        PluginCapability.HTML_OUTPUT
    )

    override suspend fun detect(input: PluginInput): DetectionResult {
        val canHandle = input.block is MermaidBlock
        return DetectionResult(canHandle = canHandle, confidence = if (canHandle) 1.0f else 0.0f)
    }

    override suspend fun render(input: PluginInput): RenderArtifact {
        val mermaidBlock = input.block as? MermaidBlock
            ?: return ErrorArtifact(id, version, "Invalid block type passed to Mermaid renderer", "")

        // The Mermaid HTML template will be loaded by WebView. We pack the diagram source as an HTML artifact.
        return HtmlArtifact(
            rendererId = id,
            rendererVersion = version,
            intrinsicWidth = null,
            intrinsicHeight = null,
            htmlBody = mermaidBlock.diagramSource
        )
    }
}
