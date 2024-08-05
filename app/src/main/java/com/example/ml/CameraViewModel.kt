package com.example.ml

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.objects.DetectedObject
import kotlinx.coroutines.launch

class CameraViewModel {
    val TAG = "ML.CameraViewModel"

    val bitmap
        get() = mutableStateBitmap.value
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
        mlObjectRecognizer = object:MlObjectRecognizer() {
            override fun on(list: List<DetectedObject>) {
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