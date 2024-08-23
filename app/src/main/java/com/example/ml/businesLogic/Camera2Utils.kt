package com.example.ml.businesLogic

import android.app.Activity
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.util.stream.Collectors
import kotlin.math.abs

class Camera2Utils {
    companion object {
        @RequiresApi(Build.VERSION_CODES.S)
        @Throws(CameraAccessException::class)
        public fun pickPreviewResolution(context: Activity, manager: CameraManager, cameraId: String) : Size {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
            val textureSizes = map!!.getOutputSizes(
                SurfaceTexture::class.java
            )
            val displaySize = Point()
            val displayMetrics = context.resources.displayMetrics
            displaySize.x = displayMetrics.widthPixels
            displaySize.y = displayMetrics.heightPixels
            if (displaySize.x < displaySize.y) {
                displaySize.x = displayMetrics.heightPixels
                displaySize.y = displayMetrics.widthPixels
            }
            val displayArRatio = displaySize.x.toFloat() / displaySize.y
            val previewSizes = ArrayList<Size>()
            for (sz in textureSizes) {
                val arRatio = sz.width.toFloat() / sz.height
                if (abs(arRatio - displayArRatio) <= .2f) {
                    previewSizes.add(sz)
                }
            }
            val currentExtension = 0
            val extensionCharacteristics = manager.getCameraExtensionCharacteristics(cameraId)
            val extensionSizes = extensionCharacteristics.getExtensionSupportedSizes(
                currentExtension, SurfaceTexture::class.java
            )
            if (extensionSizes.isEmpty()) {
                Toast.makeText(
                    context, "Invalid preview extension sizes!.",
                    Toast.LENGTH_SHORT
                ).show()
                context.finish()
            }

            var previewSize = extensionSizes[0]
            val supportedPreviewSizes =
                previewSizes.stream().distinct().filter { o: Size -> extensionSizes.contains(o) }
                    .collect(Collectors.toList())
            if (supportedPreviewSizes.isNotEmpty()) {
                var currentDistance = Int.MAX_VALUE
                for (sz in supportedPreviewSizes) {
                    val distance = abs(sz.width * sz.height - displaySize.x * displaySize.y)
                    if (currentDistance > distance) {
                        currentDistance = distance
                        previewSize = sz
                    }
                }
            } else {
                Log.w(
                    TAG, "No overlap between supported camera and extensions preview sizes using "
                            + "first available!"
                )
            }

            return previewSize
        }
    }
}