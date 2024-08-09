package com.example.ml.activity

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ml.vm.CameraViewModel
import com.example.ml.businesLogic.allRuntimePermissionsGranted
import com.example.ml.businesLogic.getRuntimePermissions
import com.example.ml.ui.theme.MLTheme
import com.google.mlkit.vision.camera.CameraXSource

class CameraActivity : ComponentActivity() {
    val TAG = "ML.CameraActivity"

    private var cameraXSource: CameraXSource? = null
    private var previewView: PreviewView? = null
    val cameraViewModel = CameraViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!allRuntimePermissionsGranted(this)) {
            getRuntimePermissions(this)
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
                        CameraPreview(
                            cameraSelector = cameraViewModel.cameraSelector,
                            scaleType = cameraViewModel.scaleType)
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)) {
                            cameraViewModel.bitmap?.asImageBitmap()?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(128.dp)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                            Canvas(
                                modifier = Modifier
                                    .padding(horizontal = 0.dp)
                                    .fillMaxSize()
                            ) {
                                cameraViewModel.screenSize = size
                                cameraViewModel.objects.forEach {
                                    drawRect(
                                        color = Color.Red,
                                        size = Size(Math.abs(it.left - it.right), Math.abs(it.top - it.bottom)),
                                        topLeft = Offset(it.left, it.top),
                                        style = Stroke(
                                            width = 3f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CameraPreview(
        cameraSelector: CameraSelector,
        scaleType: PreviewView.ScaleType,
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        AndroidView(
            modifier = Modifier,
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
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    val builder = ImageAnalysis.Builder()
                    val imageAnalysis = builder.build()
                    var i = 0
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                        { imageProxy: ImageProxy ->
                            cameraViewModel.updateImage(imageProxy)
                        })
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview
                        )
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, imageAnalysis
                        )
                    } catch (exc: Exception) {
                        Log.v(TAG, "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(context))
                previewView
            })
    }
}
