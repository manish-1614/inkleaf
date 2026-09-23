package com.inkleaf.app.domain.model

sealed interface RenderArtifact {
    val rendererId: String
    val rendererVersion: String
    val intrinsicWidth: Int?
    val intrinsicHeight: Int?
}

data class BitmapArtifact(
    override val rendererId: String,
    override val rendererVersion: String,
    override val intrinsicWidth: Int?,
    override val intrinsicHeight: Int?,
    val bitmapBytes: ByteArray
) : RenderArtifact

data class VectorArtifact(
    override val rendererId: String,
    override val rendererVersion: String,
    override val intrinsicWidth: Int?,
    override val intrinsicHeight: Int?,
    val svgString: String
) : RenderArtifact

data class HtmlArtifact(
    override val rendererId: String,
    override val rendererVersion: String,
    override val intrinsicWidth: Int?,
    override val intrinsicHeight: Int?,
    val htmlBody: String
) : RenderArtifact

data class TextArtifact(
    override val rendererId: String,
    override val rendererVersion: String,
    val text: String
) : RenderArtifact {
    override val intrinsicWidth: Int? = null
    override val intrinsicHeight: Int? = null
}

data class ErrorArtifact(
    override val rendererId: String,
    override val rendererVersion: String,
    val errorMessage: String,
    val sourceFallback: String
) : RenderArtifact {
    override val intrinsicWidth: Int? = null
    override val intrinsicHeight: Int? = null
}

data class DiagramPlaceholderArtifact(
    override val rendererId: String,
    override val rendererVersion: String,
    val diagramId: String,
    val placeholderBlock: DiagramPlaceholderBlock
) : RenderArtifact {
    override val intrinsicWidth: Int? = null
    override val intrinsicHeight: Int? = null
}
