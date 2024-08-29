package com.example.ml.businesLogic

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.InputImage.IMAGE_FORMAT_NV21
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

public abstract class Camera2ViewModel(val context : Context, val textureView: TextureView) : ViewModel() {

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
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)

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
            previewSize = Size(480, 320)
        }
        GlobalScope.launch(Dispatchers.IO) {
            cameraDevice = openCamera(cameraManager, cameraId)
            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 1)
            MainScope().launch {
                startPreview()
                imageReader.setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    val cropedImage = cropImage_YUV_420_888(image, Rect(0,0,640, 480), 90)
                    Log.v(com.example.ml.businesLogic.TAG, cropedImage?.width.toString() + "x" + cropedImage?.height.toString()+ " " + cropedImage?.format.toString())
//                    Log.v(TAG, Thread.currentThread().name)
                    image.close()
                }, imageReaderHandler)
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
        previewRequestBuilder.addTarget(imageReader.surface)
        val outputConfig = OutputConfiguration(previewSurface)
        val outputImageReaderConfig = OutputConfiguration(imageReader.surface)
        val sessionConfig = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            listOf(outputConfig, outputImageReaderConfig),
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
        cameraDevice.createCaptureSession(sessionConfig)
    }

    fun cropImage_YUV_420_888(mediaImage: Image, cropRect : Rect, rotation: Int) : Image? {
        croppedNV21(mediaImage, cropRect).let { byteArray ->
            val inputImage = InputImage.fromByteArray(
                byteArray,
                cropRect.width(),
                cropRect.height(),
                rotation,
                ImageFormat.NV21,
            )
            return inputImage.mediaImage
        }
    }

    private fun croppedNV21(mediaImage: Image, cropRect: Rect): ByteArray {
        val yBuffer = mediaImage.planes[0].buffer // Y
        val vuBuffer = mediaImage.planes[2].buffer // VU
        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()
        val nv21 = ByteArray(ySize + vuSize)
        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)
        return cropByteArray(nv21, mediaImage.width, cropRect)
    }

    private fun cropByteArray(array: ByteArray, imageWidth: Int, cropRect: Rect): ByteArray {
        val croppedArray = ByteArray(cropRect.width() * cropRect.height())
        var i = 0
        array.forEachIndexed { index, byte ->
            val x = index % imageWidth
            val y = index / imageWidth
            if (cropRect.left <= x && x < cropRect.right && cropRect.top <= y && y < cropRect.bottom) {
                croppedArray[i] = byte
                i++
            }
        }
        return croppedArray
    }
}