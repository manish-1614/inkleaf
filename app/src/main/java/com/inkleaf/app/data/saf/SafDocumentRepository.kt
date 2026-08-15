package com.inkleaf.app.data.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DocumentMetadata(
    val uriString: String,
    val displayName: String,
    val sizeBytes: Long,
    val fingerprint: String
)

class SafDocumentRepository(private val context: Context) {

    suspend fun readDocumentContent(uri: Uri): String = withContext(Dispatchers.IO) {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line).append("\n")
                    line = reader.readLine()
                }
            }
        }
        stringBuilder.toString()
    }

    suspend fun getDocumentMetadata(uri: Uri): DocumentMetadata? = withContext(Dispatchers.IO) {
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
            } catch (e: Exception) {
                // Ignore if it cannot be persisted (e.g., non-persistent SAF sources)
            }
        }

        val content = try {
            readDocumentContent(uri)
        } catch (e: Exception) {
            ""
        }
        val fingerprint = computeFingerprint(content)

        DocumentMetadata(
            uriString = uri.toString(),
            displayName = displayName,
            sizeBytes = sizeBytes,
            fingerprint = fingerprint
        )
    }

    private fun computeFingerprint(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
