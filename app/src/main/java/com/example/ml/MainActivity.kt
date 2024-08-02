package com.example.ml

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ml.ui.theme.MLTheme
import com.google.mlkit.vision.camera.CameraXSource

class MainActivity : ComponentActivity() {
    val TAG = "ML.MainActivity"
    private val REQUIRED_RUNTIME_PERMISSIONS =
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    private val PERMISSION_REQUESTS = 1

    private var cameraXSource: CameraXSource? = null
    private var previewView: PreviewView? = null
//    val bm1 = remember { mutableStateOf<Bitmap?>(null) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            MLTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box {
                        CameraPreview()
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)) {
//                            Text(text = "", Modifier.align(Alignment.Center))
//                            Image(bitmap = bm1, contentDescription = "test 1")
                        }
                    }
                }
            }
        }
    }

    private fun allRuntimePermissionsGranted(): Boolean {
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUESTS
            )
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }

    @OptIn(ExperimentalGetImage::class)
    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
        scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        AndroidView(
            modifier = modifier,
            factory = { context ->
                val previewView = PreviewView(context).apply {
                    this.scaleType = scaleType
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Preview is incorrectly scaled in Compose on some devices without this
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProvider = ProcessCameraProvider.getInstance(context)

                cameraProvider.addListener({
                    val cameraProvider = cameraProvider.get()

                    // Preview
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val builder = ImageAnalysis.Builder()
                    val analysisUseCase = builder.build()
                    analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(this),
                        { imageProxy: ImageProxy ->
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            Log.v(TAG, rotationDegrees.toString())

                            var bitmap = BitmapUtils.getBitmap(imageProxy)
//                            updateBitmap(bitmap)
                        })

                    try {
                        // Must unbind the use-cases before rebinding them.
                        cameraProvider.unbindAll()

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview
                        )
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, analysisUseCase
                        )
                    } catch (exc: Exception) {
                        Log.v(TAG, "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(context))

                previewView
            })
    }
}
