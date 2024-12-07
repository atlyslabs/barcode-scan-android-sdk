package com.atlys.barcode_scan

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeScanAnalyzer(
    private val barcodeScanner: BarcodeScanner,
    private val previewView: PreviewView,
    private val listener: ScanListener?
) : ImageAnalysis.Analyzer {

    private lateinit var bitmapBuffer: Bitmap

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.use {
            bitmapBuffer = imageProxy.toBitmap()
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            }
            val cropRect = imageProxy.cropRect
            var croppedBitmap = Bitmap.createBitmap(
                bitmapBuffer,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height()
            )
            croppedBitmap = Bitmap.createBitmap(
                croppedBitmap, 0, 0, croppedBitmap.width, croppedBitmap.height,
                matrix, true
            )
            val inputImage = InputImage.fromBitmap(croppedBitmap, 0)
            val scanTask = barcodeScanner.process(inputImage)
            val barcodes = Tasks.await(scanTask)
            if (barcodes.isEmpty()) {
                previewView.overlay.clear()
                return@use
            }
            val scaleX = previewView.width.toFloat() / croppedBitmap.width
            val scaleY = previewView.height.toFloat() / croppedBitmap.height
            val qrCodeDrawable = QrCodeDrawable(barcodes, scaleX, scaleY)
            previewView.overlay.clear()
            previewView.overlay.add(qrCodeDrawable)
            val results = barcodes.map { getBarcodeResult(croppedBitmap, it) }
            listener?.onDetected(results)
        }
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