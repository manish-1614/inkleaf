package com.inkleaf.app.domain.plugin

import com.inkleaf.app.domain.model.BlockModel
import com.inkleaf.app.domain.model.RenderArtifact

enum class PluginCapability {
    OFFLINE_ONLY,
    THEME_AWARE,
    VECTOR_OUTPUT,
    BITMAP_OUTPUT,
    HTML_OUTPUT
}

data class PluginInput(
    val block: BlockModel,
    val sourceText: String,
    val themeId: String,
    val containerWidthDp: Int
)

data class DetectionResult(
    val canHandle: Boolean,
    val confidence: Float
)

interface RenderPlugin {
    val id: String
    val version: String
    val languageTags: Set<String>
    val capabilities: Set<PluginCapability>

    suspend fun detect(input: PluginInput): DetectionResult
    suspend fun render(input: PluginInput): RenderArtifact
}
