package com.atlys.codescanner

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.atlys.codescanner.databinding.LayoutBarcodeScannerBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), DefaultLifecycleObserver {

    private var viewBinding: LayoutBarcodeScannerBinding =
        LayoutBarcodeScannerBinding.inflate(LayoutInflater.from(context), this, true)
    private val cameraController: LifecycleCameraController = LifecycleCameraController(context)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
    private lateinit var lifecycleOwner: LifecycleOwner

    fun bindLifecycle(lifecycleOwner: LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
        lifecycleOwner.lifecycle.addObserver(this)
    }

    fun startScan() {
        startCamera()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val previewView: PreviewView = viewBinding.previewView

        cameraController.setImageAnalysisAnalyzer(
            cameraExecutor,
            BarcodeScanAnalyzer(barcodeScanner, previewView)
        )

        cameraController.imageAnalysisResolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    android.util.Size(600, 600),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
            .build()

        cameraController.bindToLifecycle(lifecycleOwner)
        previewView.controller = cameraController
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleOwner.lifecycle.removeObserver(this)
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }

}