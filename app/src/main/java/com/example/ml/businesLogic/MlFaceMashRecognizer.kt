package com.example.ml.businesLogic

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.odml.image.MediaMlImageBuilder
import com.google.mlkit.vision.common.PointF3D
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
    private var leftCount = 0
    private var rightCount = 0

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
        val minTimes = 0
        val threshold = 5.0f
        list.firstOrNull()?.let {
            var left = false
//            val leftK = blinkCoefficient(it.getPoints(LEFT_EYE))
            val leftK = blinkCoefficientLeft(it)
            if(leftK > threshold) {
                leftCount++
                left = leftCount > minTimes
            } else {
                leftCount = 0
            }
//            val rightK = blinkCoefficient(it.getPoints(RIGHT_EYE))
            val rightK = blinkCoefficientRight(it)
            var right = false
            if(rightK > threshold) {
                rightCount++
                right = rightCount > minTimes
            } else {
                rightCount = 0
            }
//            Log.v(TAG, "LEFT: " + (leftK + 0.5).toInt() + " RIGHT: " + (rightK + 0.5).toInt())
//            if((left && !right) || (!left && right)) {
//                onBlink(left, right)
//            } else {
//                onBlink(false, false)
//            }
            onBlink(left, right)
        }
    }

    private fun blinkCoefficient(list : List<FaceMeshPoint>) : Float {
        var xMax = 0f
        var xMin = Float.MAX_VALUE
        var yMax = 0f
        var yMin = Float.MAX_VALUE
        var i1 = -1
        var i2 = -1
        var i3 = -1
        var i4 = -1
        list.forEach {
            if (it.position.x > xMax) {
                xMax = it.position.x
                i1 = it.index
            }
            if (it.position.x < xMin) {
                xMin = it.position.x
                i2 = it.index
            }
            if (it.position.y > yMax) {
                yMax = it.position.y
                i3 = it.index
            }
            if (it.position.y < yMin) {
                yMin = it.position.y
                i4 = it.index
            }
        }
        Log.v(TAG, "indexes : $i1 $i2 $i3 $i4" )
        return (xMax - xMin)/(yMax - yMin)
        //indexes : 133 33 145 159
        //indexes : 263 362 374 386
    }

    private fun blinkCoefficientLeft(faceMesh: FaceMesh) : Float {
        try {
            val list = faceMesh.getPoints(LEFT_EYE)
            val xMax = list.filter { it.index == 133 }.first().position
            val xMin = list.filter { it.index == 33 }.first().position
            val yMax = list.filter { it.index == 145 }.first().position
            val yMin = list.filter { it.index == 159 }.first().position
            return coeficient(xMax, xMin, yMax, yMin)
        } catch (e : NoSuchElementException) {
            return 0f
        } catch (e : Exception) {
            return 0f
        }
    }

    private fun blinkCoefficientRight(faceMesh: FaceMesh) : Float {
        try {
            val list = faceMesh.getPoints(RIGHT_EYE)
            val xMax = list.filter { it.index == 263 }.first().position
            val xMin = list.filter { it.index == 362 }.first().position
            val yMax = list.filter { it.index == 374 }.first().position
            val yMin = list.filter { it.index == 386 }.first().position
            return coeficient(xMax, xMin, yMax, yMin)
        } catch (e : NoSuchElementException) {
            return 0f
        } catch (e : Exception) {
            return 0f
        }
    }

    private fun coeficient(xMax : PointF3D, xMin : PointF3D, yMax : PointF3D, yMin : PointF3D) : Float {
        val xV = Math.sqrt(((xMax.x - xMin.x) * (xMax.x - xMin.x) + (xMax.y - xMin.y) * (xMax.y - xMin.y)).toDouble())
        val yV = Math.sqrt(((yMax.x - yMin.x) * (yMax.x - yMin.x) + (yMax.y - yMin.y) * (yMax.y - yMin.y)).toDouble())
//        Log.v(TAG, "vectors : $xV / $yV = " + (xV / yV).toFloat()  )
        return (xV / yV).toFloat()
    }
}