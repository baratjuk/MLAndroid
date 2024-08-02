package com.example.ml

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf

class CameraViewModel {
    var bitmap = mutableStateOf<Bitmap?>(null)
    init {
    }
}