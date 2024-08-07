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
import com.example.ml.businesLogic.BitmapUtils
import com.example.ml.businesLogic.MlObjectRecognizer
import com.google.mlkit.vision.objects.DetectedObject

class CameraViewModel {
    val TAG = "ML.CameraViewModel"

    val bitmap
        get() = mutableStateBitmap.value
    var mutableRect1 = mutableStateOf<RectF?>(null)

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
//        imageProxy.close()
    }

    init {
        mlObjectRecognizer = object: MlObjectRecognizer() {
            override fun on(list: List<DetectedObject>) {
                list.firstOrNull()?.let {
                    val x = it.boundingBox
                    mutableRect1.value = RectF(x.left.toFloat(), x.top.toFloat(), x.right.toFloat(), x.bottom.toFloat())
                }
                for(detectedObject in list) {
                    detectedObject.boundingBox
                    detectedObject.labels.forEach {
                        Log.v(TAG, it.text + " " + it.index + " " + it.confidence)
                    }
                }

            }
        }
    }
}