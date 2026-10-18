package com.inkleaf.app.data.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.inkleaf.app.domain.model.*
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class ArtifactCache(private val context: Context) {

    // 2MB Memory cache limit for decoded bitmap/vector assets
    private val memoryCache = object : LruCache<String, RenderArtifact>(2 * 1024 * 1024) {
        override fun sizeOf(key: String, value: RenderArtifact): Int {
            return when (value) {
                is BitmapArtifact -> value.bitmapBytes.size
                is VectorArtifact -> value.svgString.length * 2
                is HtmlArtifact -> value.htmlBody.length * 2
                else -> 1024 // Fallback 1KB
            }
        }
    }

    private val diskCacheDir = File(context.cacheDir, "inkleaf_artifacts").apply {
        if (!exists()) mkdirs()
    }

    fun computeCacheKey(
        sourceText: String,
        pluginId: String,
        pluginVersion: String,
        rendererVersion: String,
        themeId: String,
        widthBucket: Int,
        densityBucket: Int
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = sourceText + pluginId + pluginVersion + rendererVersion + themeId + widthBucket + densityBucket
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun get(key: String): RenderArtifact? {
        // 1. Try memory cache
        memoryCache.get(key)?.let { return it }

        // 2. Try disk cache
        val metadataFile = File(diskCacheDir, "$key.meta")
        if (!metadataFile.exists()) return null

        try {
            val lines = metadataFile.readLines()
            if (lines.size < 4) return null
            val type = lines[0]
            val rId = lines[1]
            val rVer = lines[2]
            val width = lines[3].toIntOrNull()
            val height = lines[4].toIntOrNull()

            val contentFile = File(diskCacheDir, "$key.dat")
            if (!contentFile.exists()) return null

            val artifact = when (type) {
                "BITMAP" -> BitmapArtifact(rId, rVer, width, height, contentFile.readBytes())
                "VECTOR" -> VectorArtifact(rId, rVer, width, height, contentFile.readText())
                "HTML" -> HtmlArtifact(rId, rVer, width, height, contentFile.readText())
                "TEXT" -> TextArtifact(rId, rVer, contentFile.readText())
                else -> null
            }

            if (artifact != null) {
                memoryCache.put(key, artifact)
            }
            return artifact
        } catch (e: Exception) {
            metadataFile.delete()
            return null
        }
    }

    fun put(key: String, artifact: RenderArtifact) {
        // Put to memory cache
        memoryCache.put(key, artifact)

        // Put to disk cache asynchronously or synchronously
        try {
            val metadataFile = File(diskCacheDir, "$key.meta")
            val contentFile = File(diskCacheDir, "$key.dat")

            val type = when (artifact) {
                is BitmapArtifact -> "BITMAP"
                is VectorArtifact -> "VECTOR"
                is HtmlArtifact -> "HTML"
                is TextArtifact -> "TEXT"
                else -> "ERROR"
            }

            metadataFile.writeText(
                "$type\n" +
                "${artifact.rendererId}\n" +
                "${artifact.rendererVersion}\n" +
                "${artifact.intrinsicWidth ?: ""}\n" +
                "${artifact.intrinsicHeight ?: ""}\n"
            )

            when (artifact) {
                is BitmapArtifact -> contentFile.writeBytes(artifact.bitmapBytes)
                is VectorArtifact -> contentFile.writeText(artifact.svgString)
                is HtmlArtifact -> contentFile.writeText(artifact.htmlBody)
                is TextArtifact -> contentFile.writeText(artifact.text)
                is ErrorArtifact -> contentFile.writeText(artifact.errorMessage)
            }
        } catch (e: Exception) {
            // Ignore write errors to prevent crashes (Failure isolation per GEMINI.md)
        }
    }

    fun clear() {
        memoryCache.evictAll()
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }
}
