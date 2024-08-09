package com.example.ml.vm

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import com.example.ml.businesLogic.BitmapUtils
import com.example.ml.businesLogic.MlObjectRecognizer
import com.google.mlkit.vision.objects.DetectedObject

class CameraViewModel {
    val TAG = "ML.CameraViewModel"

    val bitmap
        get() = mutableStateBitmap.value
    var objects = mutableListOf<RectF>()
    var screenSize : Size? = null
    var k : Float = 1f

    var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    var scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FIT_START // FILL_CENTER

    private var mutableStateBitmap = mutableStateOf<Bitmap?>(null)
    private val mlObjectRecognizer : MlObjectRecognizer

    @OptIn(ExperimentalGetImage::class)
    fun updateImage(imageProxy : ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        BitmapUtils.getBitmap(imageProxy)?.let {
            mutableStateBitmap.value =  it
        }
        imageProxy?.let {
            mlObjectRecognizer.processImage(it)
        }
        Log.v(TAG, "imageProxy: " + imageProxy.width.toString() + " " + imageProxy.height.toString())
        k = ((screenSize?.height ?: 0) as Float) / imageProxy.width
        Log.v(TAG, "imageProxy: " + k)
    }

    init {
        mlObjectRecognizer = object: MlObjectRecognizer() {
            override fun on(list: List<DetectedObject>) {
                objects.clear()
                list.forEach {
                    val x = it.boundingBox
                    k = 1f
                    val recognizedObj = RectF(
                        (k * x.left).toFloat(),
                        (k * x.top).toFloat(),
                        (k + x.right).toFloat(),
                        (k * x.bottom).toFloat())
                    objects.add(recognizedObj)
                }
                for(detectedObject in list) {
                    val x = detectedObject.boundingBox
                    Log.v(TAG, x.top.toString() + " " + x.bottom + " " + x.left  + " " + x.right )
                    detectedObject.labels.forEach {
                        Log.v(TAG, it.text + " " + it.index + " " + it.confidence)
                    }
                }

            }
        }
    }
}