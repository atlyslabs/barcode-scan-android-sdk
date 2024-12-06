package com.atlys.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

fun getFileName(context: Context, uri: Uri): String? {
    var fileName: String? = null

    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
    }

    if (fileName == null) {
        fileName = uri.path?.substring((uri.path?.lastIndexOf("/") ?: 0) + 1)
    }

    return fileName
}

fun getFileExtension(fileName: String?): String? {
    return fileName?.substringAfterLast(".", "")?.takeIf { it.isNotEmpty() }
}

fun Uri.toBitmap(context: Context): Bitmap? {
    var bitmap: Bitmap? = null
    try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(this)
        bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return bitmap
}

fun File.saveBitmap(bitmap: Bitmap) {
    this.outputStream().use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    }
}