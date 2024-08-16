package com.example.ml.vm

import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.ml.businesLogic.MlObjectRecognizer
import com.google.mlkit.vision.objects.DetectedObject

class CameraViewModel {
    val TAG = "ML.CameraViewModel"

    inner class MlObjectInfo(rect : Rect, val label: String) {
        val offset: Offset
        val size: Size
        init {
            var topOffset = (screenSize!!.height/screenSize!!.width - imageSize!!.width/imageSize!!.height) / 2 * screenSize!!.width
            val scale = screenSize!!.width / imageSize!!.height
            if(isFrontCamera) {
                size = Size(Math.abs(rect.left - rect.right).toFloat() * scale, Math.abs(rect.bottom - rect.top).toFloat() * scale)
                offset = Offset(screenSize!!.width - rect.left.toFloat() * scale - size.width, rect.top.toFloat() * scale + topOffset)
            } else {
                size = Size(Math.abs(rect.left - rect.right).toFloat() * scale, Math.abs(rect.bottom - rect.top).toFloat() * scale)
                offset = Offset(rect.left.toFloat() * scale, rect.top.toFloat() * scale + topOffset)
            }
        }

        fun color(index: Int) : Color {
            return when(index % 5) {
                0 -> Color.White
                1 -> Color.Yellow
                2 -> Color.Cyan
                3 -> Color.Red
                4 -> Color.Green
                else -> Color.Blue
            }
        }
    }

    var mlObjectsInfoList = mutableListOf<MlObjectInfo>()

    var screenSize : Size? = null
    var imageSize : Size? = null

    var cameraSelector = mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA)
    val isFrontCamera
        get() = cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA

    private val mlObjectRecognizer : MlObjectRecognizer

    init {
        mlObjectRecognizer = object: MlObjectRecognizer() {
            override fun on(list: List<DetectedObject>) {
                mlObjectsInfoList.clear()
                list.forEach {
                    val box = it.boundingBox
                    val label = it.labels.sortedBy { it.confidence }
                        .joinToString(separator = "\n", transform = {
                            it.text + " " + it.index
                        })
                    val mlObjInfo = MlObjectInfo(
                        box,
                        label
                    )
                    mlObjectsInfoList.add(mlObjInfo)
                }
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun updateImage(imageProxy : ImageProxy) {
        imageSize = Size(imageProxy.width.toFloat(), imageProxy.height.toFloat())
        imageProxy?.let {
            mlObjectRecognizer.processImage(it)
        }
//        Log.v(TAG, "imageProxy: " + imageProxy.width.toString() + " " + imageProxy.height.toString())
//        Log.v(TAG, "imageProxy: " + scale)
    }

    fun toggleCamera() {
        mlObjectsInfoList.clear()
        if (cameraSelector.value == CameraSelector.DEFAULT_FRONT_CAMERA) {
            cameraSelector.value = CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            cameraSelector.value = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }
}