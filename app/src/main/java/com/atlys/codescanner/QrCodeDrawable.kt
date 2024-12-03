package com.atlys.codescanner

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import com.google.mlkit.vision.barcode.common.Barcode

/**
 * A Drawable that handles displaying a QR Code's data and a bounding box around the QR code.
 */
class QrCodeDrawable(
    private val barcodes: List<Barcode>,
    private val scaleX: Float,
    private val scaleY: Float
) : Drawable() {
    private val boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.YELLOW
        strokeWidth = 5F
        alpha = 200
    }

    private val contentRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 255
    }

    private val contentTextPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 36F
    }

    private val contentPadding = 25

    override fun draw(canvas: Canvas) {
        barcodes.forEach { barcode ->
            drawQrBounds(canvas, barcode)
        }
    }

    private fun drawQrBounds(canvas: Canvas, barcode: Barcode) {
        val boundingBox = barcode.boundingBox!!

        // Map the bounding box coordinates to the Canvas
        val boundingRect = RectF(
            boundingBox.left * scaleX,
            boundingBox.top * scaleY,
            boundingBox.right * scaleX,
            boundingBox.bottom * scaleY
        )
        canvas.drawRect(boundingRect, boundingRectPaint)
        val textWidth = contentTextPaint.measureText(barcode.rawValue).toInt()
        canvas.drawRect(
            RectF(
                boundingRect.left,
                boundingRect.bottom + contentPadding / 2,
                boundingRect.left + textWidth + contentPadding * 2,
                boundingRect.bottom + contentTextPaint.textSize.toInt() + contentPadding
            ),
            contentRectPaint
        )
        canvas.drawText(
            barcode.rawValue.toString(),
            (boundingRect.left + contentPadding),
            (boundingRect.bottom + contentPadding * 2),
            contentTextPaint
        )
    }

    override fun setAlpha(alpha: Int) {
        boundingRectPaint.alpha = alpha
        contentRectPaint.alpha = alpha
        contentTextPaint.alpha = alpha
    }

    override fun setColorFilter(colorFiter: ColorFilter?) {
        boundingRectPaint.colorFilter = colorFilter
        contentRectPaint.colorFilter = colorFilter
        contentTextPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}