package com.atlys.codescanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.atlys.codescanner.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private var cameraExecutor: ExecutorService? = null
    private var barcodeScanner: BarcodeScanner? = null
    private val viewModel: MainViewModel by viewModels()

    private val applicationSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Handler(Looper.getMainLooper()).postDelayed({
                requestCameraPermission()
            }, 1000)
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                toast("Please grant camera permission from settings")
                if (shouldShowRequestPermissionRationale(this))
                    applicationSettings.launch(this.getApplicationDetailSettingsIntent())
            }
        }

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            // Handle the selected file URI
            uri?.let { viewModel.onFileSelected(uri) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (isPermissionGranted(Manifest.permission.CAMERA)) {
            startCamera()
        } else {
            toast("Please grant camera permission")
            requestCameraPermission()
        }
        viewBinding.buttonSelectFile.onClick {
            pickFileLauncher.launch(ValidMimeTypes.toTypedArray())
        }
        lifecycleScope.launch {
            viewModel.barcodeResults.collectLatest {
                if (it.isNotEmpty())
                    showBottomSheet(it)
            }
        }
    }

    private fun showBottomSheet(barcodeResults: List<BarcodeResult>) {
        val bottomSheet = ScanResultBottomSheetFragment()
        bottomSheet.barcodeResults = barcodeResults
        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
    }

    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        var cameraController = LifecycleCameraController(baseContext)
        val previewView: PreviewView = viewBinding.previewView

        barcodeScanner = BarcodeScanning.getClient()

        cameraController.setImageAnalysisAnalyzer(
            cameraExecutor!!,
            BarcodeScanAnalyzer(barcodeScanner!!, previewView)
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

        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        barcodeScanner?.close()
    }

}

fun Context.toast(message: String, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, message, duration).show()
}

fun Context.getApplicationDetailSettingsIntent(): Intent {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", this.packageName, null)
    intent.data = uri
    return intent
}

private fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.CAMERA
    )
}

fun View.onClick(onClick: () -> Unit) {
    this.setOnClickListener { onClick() }
}

fun File.saveBitmap(bitmap: Bitmap) {
    this.outputStream().use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    }
}