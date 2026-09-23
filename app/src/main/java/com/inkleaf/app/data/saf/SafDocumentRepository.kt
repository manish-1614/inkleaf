package com.inkleaf.app.data.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DocumentMetadata(
    val uriString: String,
    val displayName: String,
    val sizeBytes: Long,
    val fingerprint: String
)

data class LoadedDocument(
    val metadata: DocumentMetadata,
    val content: String
)

class SafDocumentRepository(private val context: Context) {

    suspend fun loadDocument(uri: Uri): LoadedDocument = withContext(Dispatchers.IO) {
        var displayName = "Unknown"
        var sizeBytes = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        // Try to persist permission if it's a content URI
        if (uri.scheme == "content") {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {
                // Ignore if it cannot be persisted
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val stringBuilder = if (sizeBytes in 1..(20 * 1024 * 1024L)) {
            StringBuilder(sizeBytes.toInt())
        } else {
            StringBuilder()
        }

        context.contentResolver.openInputStream(uri)?.use { rawIn ->
            DigestInputStream(rawIn, digest).use { dis ->
                BufferedReader(InputStreamReader(dis, Charsets.UTF_8)).use { reader ->
                    val buffer = CharArray(16384)
                    var read = reader.read(buffer)
                    while (read != -1) {
                        stringBuilder.append(buffer, 0, read)
                        read = reader.read(buffer)
                    }
                }
            }
        }

        val hashBytes = digest.digest()
        val fingerprint = hashBytes.joinToString("") { "%02x".format(it) }

        val metadata = DocumentMetadata(
            uriString = uri.toString(),
            displayName = displayName,
            sizeBytes = sizeBytes,
            fingerprint = fingerprint
        )

        LoadedDocument(metadata, stringBuilder.toString())
    }

    suspend fun readDocumentContent(uri: Uri): String = withContext(Dispatchers.IO) {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                val buffer = CharArray(16384)
                var read = reader.read(buffer)
                while (read != -1) {
                    stringBuilder.append(buffer, 0, read)
                    read = reader.read(buffer)
                }
            }
        }
        stringBuilder.toString()
    }

    suspend fun getDocumentMetadata(uri: Uri): DocumentMetadata? = withContext(Dispatchers.IO) {
        try {
            loadDocument(uri).metadata
        } catch (_: Exception) {
            null
        }
    }
}
