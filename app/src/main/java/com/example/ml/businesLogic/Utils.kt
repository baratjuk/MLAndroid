package com.example.ml.businesLogic

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.util.Log
import android.util.Size

class Utils {
    data class CameraInfo(
        val orientation: Int,
        val orientationStr: String,
        val id: String,
        val size: Size,
        val fps: Int)

    companion object {
        fun cameraInfoList(cameraManager: CameraManager) : List<CameraInfo> {
            var infoList = mutableListOf<CameraInfo>()
            cameraManager.cameraIdList.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val orientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
                val cameraConfig = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                val targetClass = MediaRecorder::class.java
                cameraConfig.getOutputSizes(targetClass).forEach { size ->
                    val secondsPerFrame =
                        cameraConfig.getOutputMinFrameDuration(targetClass, size) /
                                1_000_000_000.0
                    val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                    val orientationStr = lensOrientationString(orientation)
                    val cameraInfo = CameraInfo(orientation, orientationStr, id, size, fps)
                    infoList.add(cameraInfo)
                }
            }
            return infoList
        }

        fun cameraMaxResolution(cameraManager: CameraManager, cameraId: String) : Size {
            var maxResolution = Size(0, 0)
            var max = 0
            cameraManager.cameraIdList.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
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