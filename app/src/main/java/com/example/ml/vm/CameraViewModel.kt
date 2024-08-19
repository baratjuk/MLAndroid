package com.example.ml.vm

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR
import android.hardware.Sensor.TYPE_GYROSCOPE
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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

class CameraViewModel(val context : Context) {
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

    var rotationDegrees = 0f

    private val mlObjectRecognizer : MlObjectRecognizer
    private val sensorManager : SensorManager
    private val gyroscope : Sensor

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
        sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER) as Sensor
        sensorManager.registerListener(object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
            override fun onSensorChanged(event: SensorEvent?) {
                Log.v(TAG, "" + event?.values?.asList() ?: "+++" )
                val value0 = event?.values?.get(0) ?: return
                val value1 = event?.values?.get(1) ?: return
                if( Math.abs(value0) > Math.abs(value1) ) {
                    if(value0 > 0) {
                        Log.v(TAG, "0")
                        rotationDegrees = 90f
                    } else {
                        Log.v(TAG, "1")
                        rotationDegrees = 270f
                    }
                } else {
                    if(value1 > 0) {
                        Log.v(TAG, "2")
                        rotationDegrees = 0f
                    } else {
                        Log.v(TAG, "3")
                        rotationDegrees = 180f
                    }
                }
            }
        }, gyroscope, SensorManager.SENSOR_DELAY_UI)
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