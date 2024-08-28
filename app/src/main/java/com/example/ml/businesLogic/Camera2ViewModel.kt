package com.example.ml.businesLogic

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

public abstract class Camera2ViewModel(val context : Context, val textureView: TextureView) {

    enum class ErrorTypes(i : Int) {
        Camera(0)
    }

    abstract fun onClose()
    abstract fun onError(type: ErrorTypes, code: Int)

    private val cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest
    private lateinit var previewSurface: Surface
    private lateinit var captureSession: CameraCaptureSession

    init {
        cameraManager = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    fun open(cameraId : String) {
        textureView.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                openCameraAndStartPreview(cameraId)
            }
            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            }
            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
            }
        }
    }

    private fun openCameraAndStartPreview(cameraId : String) {
        GlobalScope.launch(Dispatchers.IO) {
            cameraDevice = openCamera(cameraManager, cameraId)
            MainScope().launch {
                startPreview()
            }
        }
    }

    fun close() {
        try {
            cameraDevice.close()
        } catch (e: Throwable) {
            Log.e(TAG, "Error closing camera", e)
        } catch (e : Exception) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
    ): CameraDevice = suspendCancellableCoroutine { cancellableContinuation ->
        manager.openCamera(cameraId, Dispatchers.IO.asExecutor(), object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = cancellableContinuation.resume(device)
            override fun onDisconnected(device: CameraDevice) = onClose()
            override fun onError(device: CameraDevice, error: Int) = onError(ErrorTypes.Camera, error)
        })
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startPreview() {
        val previewSize = Size(480, 640)
        val texture = textureView.surfaceTexture
        texture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        previewSurface = Surface(texture)
        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequestBuilder.addTarget(previewSurface)
        val outputConfig = OutputConfiguration(previewSurface)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val sessionConfig = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            listOf(outputConfig),
            mainExecutor,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    previewRequest = previewRequestBuilder.build()
                    captureSession.setRepeatingRequest(previewRequest, null, null)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    // Handle failure
                }
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraDevice.createCaptureSession(sessionConfig)
        }
    }
}