package com.example.ml.businesLogic

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.util.Log
import android.util.Size

class Utils {
    companion object {
        fun printCamerasInfo(cameraManager: CameraManager) {
            var backMaxResolution = mutableMapOf<String, Size>()
            var frontMaxResolution = mutableMapOf<String, Size>()
            cameraManager.cameraIdList.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
                val cameraConfig = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                val targetClass = MediaRecorder::class.java
                cameraConfig.getOutputSizes(targetClass).forEach { size ->
                    // Get the number of seconds that each frame will take to process
                    val secondsPerFrame =
                        cameraConfig.getOutputMinFrameDuration(targetClass, size) /
                                1_000_000_000.0
                    // Compute the frames per second to let user select a configuration
                    val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                    val fpsLabel = if (fps > 0) "$fps" else "N/A"
                    val orientationStr = lensOrientationString(orientation)
                    Log.v(TAG,"$orientationStr ($id) $size $fpsLabel FPS")
                }
            }
        }

        fun cameraMaxResolution(cameraManager: CameraManager, cameraId: String) : Size {
            var maxResolution = Size(0, 0)
            var max = 0
            cameraManager.cameraIdList.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
                val cameraConfig = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                val targetClass = MediaRecorder::class.java
                cameraConfig.getOutputSizes(targetClass).forEach { size ->
                    if (id == cameraId) {
                        val x = size.height * size.width
                        if(x > max) {
                            max = x
                            maxResolution = size
                        }
                    }
                }
            }
            return maxResolution
        }
    }
}

fun lensOrientationString(value: Int) = when (value) {
    CameraCharacteristics.LENS_FACING_BACK -> "Back"
    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
    else -> "Unknown"
}