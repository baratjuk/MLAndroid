package com.example.ml.vm

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.ml.businesLogic.MlFaceMashRecognizer
import com.example.ml.businesLogic.MlObjectRecognizer
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMesh.LEFT_EYE
import com.google.mlkit.vision.facemesh.FaceMesh.RIGHT_EYE
import com.google.mlkit.vision.objects.DetectedObject

class CameraViewModel(val context : Context) {
    val TAG = "ML.CameraViewModel"

    inner class MlObjectInfo(rect : Rect, val label: String) {
        val offset: Offset
        val size: Size
        val textOffset: Offset
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
            when (rotationDegreesMutable.value) {
                0f -> textOffset = Offset(offset.x, offset.y)
                90f -> textOffset = Offset(offset.x + size.width, offset.y)
                180f -> textOffset = Offset(offset.x + size.width, offset.y + size.height)
                270f -> textOffset = Offset(offset.x, offset.y + size.height)
                else -> textOffset = offset.copy()
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

        fun lines() = label.split("\n").size
    }

    enum class Types(i : Int) {
        LEFT_EYE(0), RIGHT_EYE(1)
    }

    inner class MlFaceMashInfo(point : PointF3D, val type: Types) {
        val offset: Offset
        init {
            var topOffset = (screenSize!!.height/screenSize!!.width - imageSize!!.width/imageSize!!.height) / 2 * screenSize!!.width
            val scale = screenSize!!.width / imageSize!!.height
            if(isFrontCamera) {
                offset = Offset(screenSize!!.width - point.x * scale, point.y * scale + topOffset)
            } else {
                offset = Offset(point.x * scale, point.y * scale + topOffset)
            }
        }
    }

    var mlObjectsInfoListMutable = mutableListOf<MlObjectInfo>()
    var cameraSelectorMutable = mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA)
    var rotationDegreesMutable = mutableStateOf(0f)

    var mlFaceMashInfoListMutable = mutableListOf<MlFaceMashInfo>()

    var screenSize : Size? = null
    var imageSize : Size? = null
    val isFrontCamera
        get() = cameraSelectorMutable.value == CameraSelector.DEFAULT_FRONT_CAMERA

    private val mlObjectRecognizer : MlObjectRecognizer
    private val mlFaceMashRecognizer : MlFaceMashRecognizer
    private val sensorManager : SensorManager
    private val gyroscope : Sensor

    init {
        mlObjectRecognizer = object: MlObjectRecognizer() {
            override fun on(list: List<DetectedObject>) {
                mlObjectsInfoListMutable.clear()
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
                    mlObjectsInfoListMutable.add(mlObjInfo)
                }
            }
        }
        mlFaceMashRecognizer = object: MlFaceMashRecognizer() {
            override fun on(list: List<FaceMesh>) {
                mlFaceMashInfoListMutable.clear()
                list.forEach {
                    val box = it.boundingBox
//                    for(point in it.allPoints) {
//                        Log.v(TAG, point.position.toString())
//                    }
                    for(point in it.getPoints(LEFT_EYE)) {
                        val mlInfo = MlFaceMashInfo(point.position, Types.LEFT_EYE)
                        mlFaceMashInfoListMutable.add(mlInfo)
                    }
                    for(point in it.getPoints(RIGHT_EYE)) {
                        val mlInfo = MlFaceMashInfo(point.position, Types.RIGHT_EYE)
                        mlFaceMashInfoListMutable.add(mlInfo)
                    }
                }
            }
        }
        sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER) as Sensor
        sensorManager.registerListener(object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
            override fun onSensorChanged(event: SensorEvent?) {
                val value0 = event?.values?.get(0) ?: return
                val value1 = event?.values?.get(1) ?: return
                var degrees = 0f
                if( Math.abs(value0) > Math.abs(value1) ) {
                    if(value0 > 0) {
                        degrees = 90f
                    } else {
                        degrees = 270f
                    }
                } else {
                    if(value1 > 0) {
                        degrees = 0f
                    } else {
                        degrees = 180f
                    }
                }
                rotationDegreesMutable.value = degrees
//                Log.v(TAG, "gyroscope: " + degrees)
            }
        }, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
    }

    @OptIn(ExperimentalGetImage::class)
    fun updateImage(imageProxy : ImageProxy) {
        imageSize = Size(imageProxy.width.toFloat(), imageProxy.height.toFloat())
        imageProxy?.let {
//            mlObjectRecognizer.processImage(it)
            mlFaceMashRecognizer.processImage(it)
        }
//        Log.v(TAG, "imageProxy: " + imageProxy.width.toString() + " " + imageProxy.height.toString())
//        Log.v(TAG, "imageProxy: " + scale)
    }

    fun toggleCamera() {
        mlObjectsInfoListMutable.clear()
        if (cameraSelectorMutable.value == CameraSelector.DEFAULT_FRONT_CAMERA) {
            cameraSelectorMutable.value = CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            cameraSelectorMutable.value = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }
}