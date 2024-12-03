package com.atlys.codescanner

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage

class BarcodeScanAnalyzer(
    private val barcodeScanner: BarcodeScanner,
    private val previewView: PreviewView
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
            val qrCodeDrawable = QrCodeDrawable(barcodes.first(), scaleX, scaleY)
            previewView.overlay.clear()
            previewView.overlay.add(qrCodeDrawable)
        }
    }
}