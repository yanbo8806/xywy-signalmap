package com.xywy.signalmap.data.importer

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

internal object ImportFileMetadata {
    fun displayName(resolver: ContentResolver, uri: Uri): String = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column)?.takeIf(String::isNotBlank) else null
        } ?: fallbackName(uri)
    } catch (_: Exception) {
        fallbackName(uri)
    }

    fun mimeType(resolver: ContentResolver, uri: Uri): String? = try {
        resolver.getType(uri)
    } catch (_: Exception) {
        null
    }

    private fun fallbackName(uri: Uri): String =
        uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank) ?: "import.bin"
}
