package com.inkleaf.app.data.diagram

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory repository mapping stable diagramId -> raw diagram source text.
 * Avoids TransactionTooLargeException during Navigation Compose transitions.
 * Lifecycle is tied to document SHA-256 fingerprinting.
 */
class DiagramRepository private constructor() {

    private val diagrams = ConcurrentHashMap<String, String>()
    private var activeDocumentFingerprint: String? = null

    companion object {
        @Volatile
        private var instance: DiagramRepository? = null

        fun getInstance(): DiagramRepository {
            return instance ?: synchronized(this) {
                instance ?: DiagramRepository().also { instance = it }
            }
        }
    }

    /**
     * Derives a stable diagram ID from SHA-256(position + sourceText).
     * Survives re-parses as long as the content and position are unchanged.
     */
    fun computeDiagramId(sourceText: String, position: Int): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$position:${sourceText.trim()}"
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.take(16).joinToString("") { "%02x".format(it) }
    }

    fun storeDiagram(diagramId: String, sourceText: String) {
        diagrams[diagramId] = sourceText
    }

    fun getDiagram(diagramId: String): String? {
        return diagrams[diagramId]
    }

    fun containsDiagram(diagramId: String): Boolean {
        return diagrams.containsKey(diagramId)
    }

    fun resetForDocument(documentFingerprint: String) {
        if (activeDocumentFingerprint != documentFingerprint) {
            diagrams.clear()
            activeDocumentFingerprint = documentFingerprint
        }
    }

    fun clear() {
        diagrams.clear()
        activeDocumentFingerprint = null
    }

    val size: Int
        get() = diagrams.size
}
