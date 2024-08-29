package com.example.ml.businesLogic

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
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
    private lateinit var imageReader: ImageReader
    private lateinit var previewSize: Size

    init {
        cameraManager = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    fun open(cameraId : String) {
        textureView.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    openCameraAndStartPreview(cameraId)
                }
            }
            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            }
            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun openCameraAndStartPreview(cameraId : String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            previewSize = Camera2Utils.pickPreviewResolution(context, cameraManager, cameraId)
        } else {
            previewSize = Size(480, 640)
        }
        GlobalScope.launch(Dispatchers.IO) {
            cameraDevice = openCamera(cameraManager, cameraId)
            MainScope().launch {
                startPreview()
            }
        }
    }

    fun close() {
        try {
            captureSession.close()
        } catch (e: Throwable) {
        } catch (e : Exception) {
        }
        try {
            cameraDevice.close()
        } catch (e: Throwable) {
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
        val texture = textureView.surfaceTexture
        texture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        previewSurface = Surface(texture)
        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequestBuilder.addTarget(previewSurface)
        val outputConfig = OutputConfiguration(previewSurface)
        val sessionConfig = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            listOf(outputConfig),
            ContextCompat.getMainExecutor(context),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    previewRequest = previewRequestBuilder.build()
                    captureSession.setRepeatingRequest(previewRequest, null, null)
                    Log.v(TAG, "onConfigured")
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

    fun captureStillPicture() {
        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            // Process the image here
            image.close()
        }, null)
        val captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureBuilder.addTarget(imageReader.surface)

        // Orientation
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90)

        val captureCallback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
            }
        }

        captureSession.stopRepeating()
        captureSession.capture(captureBuilder.build(), captureCallback, null)
    }
}