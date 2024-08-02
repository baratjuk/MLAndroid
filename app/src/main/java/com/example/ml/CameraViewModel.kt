package com.example.ml

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf

class CameraViewModel {
    var bitmap
        get() = mutableBitmap.value
        set(value) {
            mutableBitmap.value = value
        }
    private var mutableBitmap = mutableStateOf<Bitmap?>(null)
    init {
    }
}