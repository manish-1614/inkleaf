package com.inkleaf.app.domain.plugin

import com.inkleaf.app.domain.model.*

class KatexRenderPlugin : RenderPlugin {

    override val id: String = "katex_webview_renderer"
    override val version: String = "0.16.9"
    override val languageTags: Set<String> = setOf("math", "latex")
    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability.OFFLINE_ONLY,
        PluginCapability.HTML_OUTPUT
    )

    override suspend fun detect(input: PluginInput): DetectionResult {
        val canHandle = input.block is MathBlock
        return DetectionResult(canHandle = canHandle, confidence = if (canHandle) 1.0f else 0.0f)
    }

    override suspend fun render(input: PluginInput): RenderArtifact {
        val mathBlock = input.block as? MathBlock
            ?: return ErrorArtifact(id, version, "Invalid block type passed to KaTeX renderer", "")

        return HtmlArtifact(
            rendererId = id,
            rendererVersion = version,
            intrinsicWidth = null,
            intrinsicHeight = null,
            htmlBody = mathBlock.latex
        )
    }
}
