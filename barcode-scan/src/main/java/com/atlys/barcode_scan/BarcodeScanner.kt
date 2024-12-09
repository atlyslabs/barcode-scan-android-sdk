package com.atlys.barcode_scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BarcodeScanner(private val context: Context, private val listener: ScanListener) {

    private val barcodeScanner = BarcodeScanning.getClient()

    suspend fun onScanUri(uri: Uri) = withContext(Dispatchers.IO) {
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

    private fun scanImage(uri: Uri) {
        val bitmap = uri.toBitmap(context)!!
        val results = detectBarCode(bitmap)
        listener.onDetected(results)
    }

    private suspend fun scanPdf(uri: Uri) {
        try {
            val bitmaps = getPdfBitmaps(uri)
            val results = bitmaps.flatMap { detectBarCode(it) }
            listener.onDetected(results)
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

interface ScanListener {
    fun onDetected(results: List<BarcodeResult>)
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

data class BarcodeResult(val bitmap: Bitmap?, val rawValue: String)