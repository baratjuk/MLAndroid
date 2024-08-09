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
import androidx.compose.ui.graphics.Color
import com.example.ml.businesLogic.BitmapUtils
import com.example.ml.businesLogic.MlObjectRecognizer
import com.google.mlkit.vision.objects.DetectedObject

data class MlObjectInfo(val rectOffset: Offset, val rectSize: Size, val label: String) {
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

class CameraViewModel {
    val TAG = "ML.CameraViewModel"
    var mlObjectsInfoList = mutableListOf<MlObjectInfo>()

    var screenSize : Size? = null
    var scale : Float = 1f

    var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA //DEFAULT_BACK_CAMERA
    var previewScaleType: PreviewView.ScaleType = PreviewView.ScaleType.FIT_START //FILL_CENTER

    private val mlObjectRecognizer : MlObjectRecognizer

    @OptIn(ExperimentalGetImage::class)
    fun updateImage(imageProxy : ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        scale = ((screenSize?.width ?: 0) as Float) / imageProxy.height
        imageProxy?.let {
            mlObjectRecognizer.processImage(it)
        }
        Log.v(TAG, "imageProxy: " + imageProxy.width.toString() + " " + imageProxy.height.toString())
        Log.v(TAG, "imageProxy: " + scale)
    }

    init {
        mlObjectRecognizer = object: MlObjectRecognizer() {
            override fun on(list: List<DetectedObject>) {
                mlObjectsInfoList.clear()
                list.forEach {
                    val box = it.boundingBox
                    val offset = Offset(box.left.toFloat() * scale, box.top.toFloat() * scale)
                    val size = Size(Math.abs(box.left - box.right).toFloat() * scale, Math.abs(box.bottom - box.top).toFloat() * scale)
                    val label = it.labels.joinToString(separator = "\n", transform = {
                        it.text + " " + it.index // + " " + it.confidence
                    })
                    val mlObjInfo = MlObjectInfo(
                        offset,
                        size,
                        label)
                    mlObjectsInfoList.add(mlObjInfo)
                }
            }
        }
    }
}