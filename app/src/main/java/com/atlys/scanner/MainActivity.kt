package com.atlys.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.atlys.scanner.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    private val scanListener = object : ScanListener {
        override fun onDetected(results: List<BarcodeResult>) {
            if (results.isNotEmpty())
                showBottomSheet(results)
        }
    }
    private val barcodeScanner = BarcodeScanner(this, scanListener)

    private val applicationSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Handler(Looper.getMainLooper()).postDelayed({
                requestCameraPermission()
            }, 1000)
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewBinding.barcodeScannerView.startScan()
            } else {
                toast("Please grant camera permission from settings")
                if (shouldShowRequestPermissionRationale(this))
                    applicationSettings.launch(this.getApplicationDetailSettingsIntent())
            }
        }

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                lifecycleScope.launch {
                    barcodeScanner.onScanUri(uri)
                }
            }
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
        setupBarcodeScannerView()
        setupOnClickListener()
    }

    private fun setupBarcodeScannerView() {
        viewBinding.barcodeScannerView.init(this, scanListener)
        if (isPermissionGranted(Manifest.permission.CAMERA)) {
            viewBinding.barcodeScannerView.startScan()
        } else {
            toast("Please grant camera permission")
            requestCameraPermission()
        }
    }

    private fun setupOnClickListener() {
        viewBinding.buttonSelectFile.onClick {
            filePicker.launch(ValidMimeTypes.toTypedArray())
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