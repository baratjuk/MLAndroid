package com.example.ml.businesLogic

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.odml.image.MediaMlImageBuilder
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class MlFaceMashRecognizer {
    val TAG = "ML.MlFaceMashRecognizer"

    abstract fun onDetect(list: List<FaceMesh>)

    private val detector: FaceMeshDetector

    init {
        val optionsBuilder = FaceMeshDetectorOptions.Builder()
//        optionsBuilder.setUseCase(FaceMeshDetectorOptions.BOUNDING_BOX_ONLY)
        detector = FaceMeshDetection.getClient(optionsBuilder.build())
    }

    @ExperimentalGetImage
    fun processImage(image : ImageProxy, exit: (image : ImageProxy)->Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            val mlImage =
                image.image?.let {
                    MediaMlImageBuilder(it)
                        .setRotation(image.imageInfo.rotationDegrees)
                        .build()
                }
            mlImage?.let {
                detector.process(it)
                    .addOnSuccessListener { result ->
                        onDetect(result)
                        exit(image)
                    }
                    .addOnFailureListener { e ->
                        exit(image)
                    }
            }
        }
    }
}