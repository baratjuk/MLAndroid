package com.example.ml

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf

class CameraViewModel {
    val bitmap
        get() = mutableStateBitmap.value
    var imageProxy : ImageProxy
        get() = imageProxy
        @OptIn(ExperimentalGetImage::class)
        set(value) {
//            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            BitmapUtils.getBitmap(value)?.let {
                mutableStateBitmap.value =  it
            }
        }
    var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    var scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FIT_START // FILL_CENTER

    private var mutableStateBitmap = mutableStateOf<Bitmap?>(null)

    init {
    }
}