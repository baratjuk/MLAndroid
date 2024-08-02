package com.example.ml

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf

class CameraViewModel {
    var bitmap
        get() = mutableStateBitmap.value
        set(value) {
            mutableStateBitmap.value = value
        }
    private var mutableStateBitmap = mutableStateOf<Bitmap?>(null)
    init {
    }
}