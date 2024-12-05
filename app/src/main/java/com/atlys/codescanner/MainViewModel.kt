package com.atlys.codescanner

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    private val barcodeScanner = BarcodeScanning.getClient()

    val barcodeResults = MutableSharedFlow<List<BarcodeResult>>()

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = getFileName(context, uri)
            val fileExtension = getFileExtension(fileName)
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType == MimeType.PDF || (mimeType == MimeType.OCTET_STREAM && fileExtension == "pdf"))
                scanPdf(uri)
            else if (ImageMimeTypes.contains(mimeType))
                scanImage(uri)
            else
                withContext(Dispatchers.Main) {
                    context.toast("Unsupported file format")
                }
        }
    }

    private suspend fun scanImage(uri: Uri) {
        val bitmap = uri.toBitmap(context)!!
        val results = detectBarCode(bitmap)
        barcodeResults.emit(results)
    }

    private suspend fun scanPdf(uri: Uri) {
        try {
            val bitmaps = getPdfBitmaps(uri)
            val results = bitmaps.flatMap { detectBarCode(it) }
            barcodeResults.emit(results)
        } catch (e: SecurityException) {
            withContext(Dispatchers.Main) {
                context.toast("PDF file is password protected")
            }
        }
    }

    private fun getPdfBitmaps(pdfUri: Uri): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val fileDescriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
        val pdfRenderer = PdfRenderer(fileDescriptor!!)

        val scaleFactor = 2
        for (pageIndex in 0 until pdfRenderer.pageCount) {
            val page = pdfRenderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(
                page.width * scaleFactor,
                page.height * scaleFactor,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            canvas.scale(scaleFactor.toFloat(), scaleFactor.toFloat())
            page.render(bitmap, null, null, Page.RENDER_MODE_FOR_DISPLAY)
            bitmaps.add(bitmap)
            page.close()
        }
        pdfRenderer.close()
        fileDescriptor.close()
        return bitmaps
    }

    private fun detectBarCode(bitmap: Bitmap): List<BarcodeResult> {
        val scanTask = barcodeScanner.process(bitmap, 0)
        val barcodes = Tasks.await(scanTask)
        return barcodes.map { getBarcodeResult(bitmap, it) }
    }

    private fun getBarcodeResult(bitmap: Bitmap, barcode: Barcode): BarcodeResult {
        val boundingBox = barcode.boundingBox!!

        // Ensure the bounding box stays within the bitmap bounds
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            boundingBox.left.coerceIn(0, bitmap.width),
            boundingBox.top.coerceIn(0, bitmap.height),
            boundingBox.width().coerceIn(0, bitmap.width - boundingBox.left),
            boundingBox.height().coerceIn(0, bitmap.height - boundingBox.top)
        )

        return BarcodeResult(
            bitmap = croppedBitmap,
            rawValue = barcode.rawValue!!
        )
    }

}

object MimeType {
    const val BMP = "image/bmp"
    const val JPEG = "image/jpeg"
    const val PNG = "image/png"
    const val GIF = "image/gif"
    const val TIFF = "image/tiff"
    const val PDF = "application/pdf"

    // Some pdf files have this type
    const val OCTET_STREAM = "binary/octet-stream"
}

val ImageMimeTypes = setOf(
    MimeType.BMP,
    MimeType.JPEG,
    MimeType.PNG,
    MimeType.GIF,
    MimeType.TIFF,
)

val PdfMimeTypes = setOf(
    MimeType.PDF,
    MimeType.OCTET_STREAM
)

val ValidMimeTypes = ImageMimeTypes + PdfMimeTypes

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

data class BarcodeResult(val bitmap: Bitmap, val rawValue: String)

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