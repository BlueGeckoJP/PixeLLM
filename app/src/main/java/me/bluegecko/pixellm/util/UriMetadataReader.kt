package me.bluegecko.pixellm.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import me.bluegecko.pixellm.model.UriMetadata

fun readUriMetadata(context: Context, uri: Uri): UriMetadata? {
    return context.contentResolver.query(
        uri, arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE
        ), null, null, null
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it != -1 }
            ?: return@use null
        val sizeIndex =
            cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it != -1 } ?: return@use null

        if (cursor.moveToFirst()) {
            val rawName = cursor.getString(nameIndex)
            val name = rawName
                .replace("^[^a-zA-Z0-9]".toRegex(), "_")
                .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val size = cursor.getLong(sizeIndex)
            val extension = rawName.substringAfterLast('.', missingDelimiterValue = "")
                .takeIf { it.isNotBlank() }?.lowercase()
            UriMetadata(name, size, extension ?: "")
        } else {
            null
        }
    }
}
