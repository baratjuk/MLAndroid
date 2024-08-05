package com.example.ml

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf

class CameraViewModel {
    var bitmap
        get() = mutableStateBitmap.value
        set(value) {
            mutableStateBitmap.value = value
        }
    var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    var scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FIT_START // FILL_CENTER

    private var mutableStateBitmap = mutableStateOf<Bitmap?>(null)

    init {
    }
}