package com.example.ml.vm

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.YuvImage
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
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
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.ml.businesLogic.MlObjectRecognizer
import com.example.ml.businesLogic.TAG
import com.example.ml.businesLogic.Utils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume


public abstract class Camera2ViewModel(val context : Context, val textureView: TextureView) : ViewModel() {

    enum class ErrorTypes(i : Int) {
        Camera(0)
    }

    abstract fun onOpen(previewSize: Size)
    abstract fun onBitmap(bitmap: Bitmap)
    abstract fun onClose()
    abstract fun onError(type: ErrorTypes, code: Int)

    private val cameraManager: CameraManager
    private lateinit var previewSize: Size
    private lateinit var cameraDevice: CameraDevice
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest
    private lateinit var previewSurface: Surface
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)
    private val mlObjectRecognizer : MlObjectRecognizer
    private var isBusy = false
    private lateinit var centralRect: Rect

    init {
        cameraManager = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Utils.cameraInfoList(cameraManager).forEach {
            Log.v(TAG, "${it.orientationStr} ${it.id} ${it.size} ${it.fps}")
        }
        mlObjectRecognizer = object: MlObjectRecognizer() {
            override fun onDetect(list: List<DetectedObject>) {
                list.forEach {
                    val box = it.boundingBox
                    val label = it.labels.sortedBy { it.confidence }
                        .joinToString(separator = "\n", transform = {
                            it.text + " " + it.index
                        })
                    Log.v(TAG, label)
                }
            }
        }
    }

    fun open(cameraId : String) {
        textureView.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    openCameraAndStartPreview(cameraId)
                    onOpen(previewSize)
                }
            }
            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            }
            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
            }
        }
    }

    @ExperimentalGetImage
    @RequiresApi(Build.VERSION_CODES.P)
    private fun openCameraAndStartPreview(cameraId : String) {
        previewSize = Utils.cameraMaxResolution(cameraManager, cameraId)
        val centerX = previewSize.width/2
        val centerY = previewSize.height/2
        centralRect = Rect(centerX - 240, centerY - 320, centerX + 240, centerY + 320)
        GlobalScope.launch(Dispatchers.IO) {
            cameraDevice = openCamera(cameraManager, cameraId)
            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 1) // YUV_420_888
            MainScope().launch {
                startPreview()
                imageReader.setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    val cropedList = cropImages_YUV_420_888(image, 180)
//                    val cropedBitmap = cropImage_YUV_420_888(image, centralRect)
                    image.close()
                    if(isBusy) {
                        return@setOnImageAvailableListener
                    }
                    isBusy = true
                    MainScope().launch {
                        val inputImage = cropedList[0]
                        inputImage.bitmapInternal?.let {
                            onBitmap(it)
                        }
                    }
                    requestAllImages(cropedList)
                }, imageReaderHandler)
            }
        }
    }

    @ExperimentalGetImage
    private fun requestAllImages(cropedList: List<InputImage>, count: Int = cropedList.size - 1) {
        if(count < 0) {
            isBusy = false
            return
        }
        mlObjectRecognizer.processImage(cropedList[count]) {
            requestAllImages(cropedList, count = count - 1)
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

    private fun cropImage_YUV_420_888(mediaImage: Image, cropRect: Rect): Bitmap {
        val yBuffer = mediaImage.planes[0].buffer // Y
        val vuBuffer = mediaImage.planes[2].buffer // VU
        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()
        val nv21 = ByteArray(ySize + vuSize)
        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, mediaImage.width, mediaImage.height, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(cropRect, 100, outputStream)
        val imageBytes = outputStream.toByteArray()
        val cropedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        return cropedBitmap
    }

    private fun cropImages_YUV_420_888(mediaImage: Image, rotationDegrees: Int): List<InputImage> {
        val yBuffer = mediaImage.planes[0].buffer // Y
        val vuBuffer = mediaImage.planes[2].buffer // VU
        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()
        val nv21 = ByteArray(ySize + vuSize)
        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, mediaImage.width, mediaImage.height, null)
        val outputStream = ByteArrayOutputStream()
        var list = mutableListOf<InputImage>()
        val W = 640
        val H = 480
        Log.v(TAG, previewSize.toString())
        for(x in 0..(previewSize.width - W) step W) {
            for(y in 0..(previewSize.height - H) step H) {
                var cropRect = Rect(x, y, W + x, H + y)
//                Log.v(TAG, cropRect.toString())
                yuvImage.compressToJpeg(cropRect, 100, outputStream)
                val imageBytes = outputStream.toByteArray()
                val cropedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val cropedRotatedInputImage = InputImage.fromBitmap(cropedBitmap, rotationDegrees)
                list.add(cropedRotatedInputImage)
            }
        }
        return list
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationAngle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationAngle)
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.getWidth(),
            bitmap.getHeight(),
            matrix,
            true
        )
        return rotatedBitmap
    }

    private fun convertImageToBitmap(image: Image): Bitmap {
        val buffer: ByteBuffer = image.planes.get(0).buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val bitmapImage: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        return bitmapImage
    }
}