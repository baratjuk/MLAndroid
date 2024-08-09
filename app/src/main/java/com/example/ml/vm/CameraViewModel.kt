package com.example.ml.vm

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.ml.businesLogic.BitmapUtils
import com.example.ml.businesLogic.MlObjectRecognizer
import com.google.mlkit.vision.objects.DetectedObject

data class MlObjectInfo(val rectOffset: Offset, val rectSize: Size, val label: String)

class CameraViewModel {
    val TAG = "ML.CameraViewModel"
    val bitmap
        get() = mutableStateBitmap.value
    var mlObjectsInfoList = mutableListOf<MlObjectInfo>()

    var screenSize : Size? = null
    var k : Float = 1f

    var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    var scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FIT_START // FILL_CENTER

    private var mutableStateBitmap = mutableStateOf<Bitmap?>(null)
    private val mlObjectRecognizer : MlObjectRecognizer

    @OptIn(ExperimentalGetImage::class)
    fun updateImage(imageProxy : ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        BitmapUtils.getBitmap(imageProxy)?.let {
            mutableStateBitmap.value =  it
        }
        imageProxy?.let {
            mlObjectRecognizer.processImage(it)
        }
        Log.v(TAG, "imageProxy: " + imageProxy.width.toString() + " " + imageProxy.height.toString())
        k = ((screenSize?.height ?: 0) as Float) / imageProxy.width
        Log.v(TAG, "imageProxy: " + k)
    }

    init {
        mlObjectRecognizer = object: MlObjectRecognizer() {
            override fun on(list: List<DetectedObject>) {
                mlObjectsInfoList.clear()
                list.forEach {
                    val x = it.boundingBox
                    val label = it.labels.joinToString(separator = ";", transform = {
                        it.text + " " + it.index + " " + it.confidence
                    })
                    val mlObjInfo = MlObjectInfo(
                        Offset(Math.min(x.right, x.left).toFloat(), x.top.toFloat()),
                        Size(Math.abs(x.left - x.right).toFloat(), Math.abs(x.bottom - x.top).toFloat()),
                        label)
                    mlObjectsInfoList.add(mlObjInfo)
                }
                for(detectedObject in list) {
                    val x = detectedObject.boundingBox
                    Log.v(TAG, x.top.toString() + " " + x.bottom + " " + x.left  + " " + x.right )
                    detectedObject.labels.forEach {
                        Log.v(TAG, it.text + " " + it.index + " " + it.confidence)
                    }
                }

            }
        }
    }
}