package com.example.ml

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.odml.image.MediaMlImageBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class MlObjectRecognizer {
    abstract fun on()

    init {

    }

    @ExperimentalGetImage
    fun processImage(image : ImageProxy) {
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                val mlImage =
                    image.image?.let {
                        MediaMlImageBuilder(it)
                            .setRotation(image.imageInfo.rotationDegrees)
                            .build()
                    }
                delay(500)
            }
            image.close()
            on()
        }
    }
}