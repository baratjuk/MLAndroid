package com.example.ml

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.odml.image.MediaMlImageBuilder
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class MlObjectRecognizer {
    val TAG = "ML.MlObjectRecognizer"

    abstract fun on()

    private val objectDetector: ObjectDetector
    private val localModel: LocalModel

    init {
        localModel = LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite").build()
        val builder: CustomObjectDetectorOptions.Builder =
            CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE) // CustomObjectDetectorOptions.SINGLE_IMAGE_MODE
        builder.enableMultipleObjects()
        builder.enableClassification().setMaxPerObjectLabelCount(5)
        val options = builder.build()
        objectDetector = ObjectDetection.getClient(options)
    }

    @ExperimentalGetImage
    fun processImage(image : ImageProxy) {
        GlobalScope.launch(Dispatchers.Main) {
            val mlImage =
                image.image?.let {
                    MediaMlImageBuilder(it)
                        .setRotation(image.imageInfo.rotationDegrees)
                        .build()
                }
            mlImage?.let {
                objectDetector.process(it)
                    .addOnSuccessListener { res ->
                        Log.v(TAG, res.size.toString())
                        on()
                        image.close()
                    }
                    .addOnFailureListener { e ->

                    }
            }
        }
    }
}