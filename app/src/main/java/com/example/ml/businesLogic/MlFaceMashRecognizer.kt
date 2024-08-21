package com.example.ml.businesLogic

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.odml.image.MediaMlImageBuilder
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMesh.LEFT_EYE
import com.google.mlkit.vision.facemesh.FaceMesh.RIGHT_EYE
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class MlFaceMashRecognizer {
    val TAG = "ML.MlFaceMashRecognizer"

    abstract fun onDetect(list: List<FaceMesh>)
    abstract fun onBlink(left:Boolean, right:Boolean)

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
                        detectClip(result)
                        exit(image)
                    }
                    .addOnFailureListener { e ->
                        exit(image)
                    }
            }
        }
    }

    fun detectClip(list: List<FaceMesh>) {
        list.firstOrNull()?.let {
            val leftK = blinkCoefficient(it.getPoints(LEFT_EYE))
            val rightK = blinkCoefficient(it.getPoints(RIGHT_EYE))
            Log.v(TAG, "LEFT: " + leftK + "RIGHT: " + rightK)
            onBlink(leftK > 5f, rightK > 5f)
        }
    }

    private fun blinkCoefficient(list : List<FaceMeshPoint>) : Float {
        var xMax = 0f
        var xMin = Float.MAX_VALUE
        var yMax = 0f
        var yMin = Float.MAX_VALUE
        list.forEach {
            if (it.position.x > xMax) {
                xMax = it.position.x
            }
            if (it.position.x < xMin) {
                xMin = it.position.x
            }
            if (it.position.y > yMax) {
                yMax = it.position.y
            }
            if (it.position.y < yMin) {
                yMin = it.position.y
            }
        }
        return (xMax - xMin)/(yMax - yMin)
    }
}