package com.example.ml.activity

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ml.vm.CameraViewModel
import com.example.ml.businesLogic.allRuntimePermissionsGranted
import com.example.ml.businesLogic.getRuntimePermissions
import com.example.ml.ui.theme.MLTheme
import com.google.mlkit.vision.camera.CameraXSource

class CameraActivity : ComponentActivity() {
    val TAG = "ML.CameraActivity"

    val cameraViewModel = CameraViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!allRuntimePermissionsGranted(this)) {
            getRuntimePermissions(this)
        }

//        val cameraX = CameraX.getOrCreateInstance(this)

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
                            cameraSelector = cameraViewModel.cameraSelector.value,
                            scaleType = cameraViewModel.previewScaleType.value)
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)) {
                            val textMeasurer = rememberTextMeasurer()
                            Canvas(
                                modifier = Modifier
                                    .padding(horizontal = 0.dp)
                                    .fillMaxSize()
                            ) {
                                cameraViewModel.screenSize = size
                                cameraViewModel.mlObjectsInfoList.forEachIndexed { index, item ->
                                    drawRect(
                                        color = item.color(index),
                                        size = item.rectSize,
                                        topLeft = item.rectOffset,
                                        style = Stroke(
                                            width = 2f
                                        )
                                    )
                                    val style = TextStyle(
                                        fontSize = 18.sp,
                                        color = item.color(index),
                                        background = Color.Transparent
                                    )
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = item.label,
                                        style = style,
                                        topLeft = item.rectOffset)
                                }
                            }
                            Column(Modifier.align(Alignment.BottomCenter)) {
                                Button(
                                    onClick = {
                                        cameraViewModel.toggleCamera()
                                        recreate()
                                    },
                                    modifier = Modifier
                                        .padding(end = 16.dp),
                                    content = { Text( if (true)  "Front" else "Back") },
                                    colors = ButtonDefaults.buttonColors(
                                        contentColor = Color.White,
                                        containerColor = Color.Black
                                    )
                                )
                                Button(
                                    onClick = {
                                        cameraViewModel.togglePreviewScale()
                                    },
                                    modifier = Modifier
                                        .padding(end = 16.dp),
                                    content = { Text( if (true)  "Fit" else "Fill") },
                                    colors = ButtonDefaults.buttonColors(
                                        contentColor = Color.White,
                                        containerColor = Color.Black
                                    )
                                )
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
                    scaleX = cameraViewModel.previewScaleX
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
            }, update = {
                it.scaleType = cameraViewModel.previewScaleType.value
            })
    }
}
