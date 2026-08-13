package com.inkleaf.app.domain.plugin

import com.inkleaf.app.domain.model.BlockModel
import com.inkleaf.app.domain.model.RenderArtifact
import com.inkleaf.app.domain.model.SvgBlock
import com.inkleaf.app.domain.model.VectorArtifact
import com.inkleaf.app.domain.model.ErrorArtifact
import java.util.regex.Pattern

class SvgRenderPlugin : RenderPlugin {

    override val id: String = "svg_native_renderer"
    override val version: String = "1.0.0"
    override val languageTags: Set<String> = setOf("svg")
    override val capabilities: Set<PluginCapability> = setOf(
        PluginCapability.OFFLINE_ONLY,
        PluginCapability.VECTOR_OUTPUT
    )

    override suspend fun detect(input: PluginInput): DetectionResult {
        val canHandle = input.block is SvgBlock
        return DetectionResult(canHandle = canHandle, confidence = if (canHandle) 1.0f else 0.0f)
    }

    override suspend fun render(input: PluginInput): RenderArtifact {
        val svgBlock = input.block as? SvgBlock 
            ?: return ErrorArtifact(id, version, "Invalid block type passed to SVG renderer", "")

        val sanitizedSvg = sanitizeSvgContent(svgBlock.svgContent)
        
        return VectorArtifact(
            rendererId = id,
            rendererVersion = version,
            intrinsicWidth = null,
            intrinsicHeight = null,
            svgString = sanitizedSvg
        )
    }

    /**
     * Sanitizes SVG input by stripping out scripts, javascript triggers, and external remote resources.
     */
    fun sanitizeSvgContent(rawSvg: String): String {
        var clean = rawSvg

        // 1. Remove script elements
        val scriptPattern = Pattern.compile("<script.*?>.*?</script>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        clean = scriptPattern.matcher(clean).replaceAll("")

        // 2. Remove inline event handlers (onclick, onload, onerror, etc.)
        val eventPattern = Pattern.compile("\\bon[a-z]+\\s*=\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE)
        clean = eventPattern.matcher(clean).replaceAll("")

        // 3. Strip hrefs matching external URLs (http:// or https://)
        val remoteUrlPattern = Pattern.compile("href\\s*=\\s*\"https?://[^\"]*\"", Pattern.CASE_INSENSITIVE)
        clean = remoteUrlPattern.matcher(clean).replaceAll("href=\"#\"")

        return clean
    }
}
