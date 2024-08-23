package com.example.ml.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ExtensionSessionConfiguration
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.example.ml.businesLogic.Camera2Utils
import com.example.ml.vm.CameraViewModel
import com.example.ml.businesLogic.allRuntimePermissionsGranted
import com.example.ml.businesLogic.getRuntimePermissions
import com.example.ml.databinding.ActivityCamera2Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Camera2Activity : ComponentActivity() {
    val TAG = "ML.Camera2Activity"

    private lateinit var binding: ActivityCamera2Binding
    private var cameraId = "1"

    private val cameraManager: CameraManager by lazy {
        val context = this.applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraExtensionSession: CameraExtensionSession
    private lateinit var previewSurface: Surface

    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.IO) {
        // Open the selected camera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraDevice = openCamera(cameraManager, cameraId)
        }

        startPreview()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        if (!allRuntimePermissionsGranted(this)) {
            getRuntimePermissions(this)
        }

        cameraManager.cameraIdList.forEach { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val orientation = lensOrientationString(
                characteristics.get(CameraCharacteristics.LENS_FACING)!!)
            Log.v(TAG, "$orientation  $id")
        }

        binding = ActivityCamera2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.texture.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                initializeCamera()
            }
            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            }
            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                return true
            }
            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
            }
        }

//        initializeCamera()
    }

    override fun onDestroy() {
        try {
            cameraDevice.close()
        } catch (e: Throwable) {
            Log.e(TAG, "Error closing camera", e)
        } catch (e : Exception) {
        }
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, Dispatchers.IO.asExecutor(), object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = cont.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                Log.w(TAG, "Camera $cameraId has been disconnected")
                finish()
            }

            override fun onError(device: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                Log.e(TAG, exc.message, exc)
                if (cont.isActive) cont.resumeWithException(exc)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startPreview() {

        val previewSize = Camera2Utils.pickPreviewResolution(this, cameraManager, cameraId)
        previewSurface = createPreviewSurface(previewSize)
//        stillImageReader = createStillImageReader()
//        postviewImageReader = createPostviewImageReader()
//        isPostviewAvailable = postviewImageReader != null

        val outputConfig = ArrayList<OutputConfiguration>()
//        outputConfig.add(OutputConfiguration(stillImageReader.surface))
        outputConfig.add(OutputConfiguration(previewSurface))
        val extensionConfiguration = ExtensionSessionConfiguration(
            0, outputConfig,
            Dispatchers.IO.asExecutor(), object : CameraExtensionSession.StateCallback() {
                override fun onClosed(session: CameraExtensionSession) {
                    cameraDevice.close()
                }

                override fun onConfigured(session: CameraExtensionSession) {
                    cameraExtensionSession = session
                    submitRequest(
                        CameraDevice.TEMPLATE_PREVIEW,
                        previewSurface,
                        true
                    ) { request ->
                        request.apply {
                            set(CaptureRequest.CONTROL_ZOOM_RATIO, 1f)
                        }
                    }
                }

                override fun onConfigureFailed(session: CameraExtensionSession) {
                    Log.v(TAG,"Failed to start camera extension preview.")
                }
            }
        )

        try {
            cameraDevice.createExtensionSession(extensionConfiguration)
        } catch (e: CameraAccessException) {
            Log.v(TAG, "Failed during extension initialization!.")
        }
    }

    private fun lensOrientationString(value: Int) = when (value) {
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
        else -> "Unknown"
    }

    private fun createPreviewSurface(previewSize: Size): Surface {
        val texture = binding.texture.surfaceTexture
        texture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        return Surface(texture)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun submitRequest(
        templateType: Int,
        targets: List<Surface>,
        isRepeating: Boolean,
        block: (captureRequest: CaptureRequest.Builder) -> CaptureRequest.Builder) {
        try {
            val captureBuilder = cameraDevice.createCaptureRequest(templateType)
                .apply {
                    targets.forEach {
                        addTarget(it)
                    }
                    block(this)
                }
            if (isRepeating) {
                cameraExtensionSession.setRepeatingRequest(
                    captureBuilder.build(),
                    Dispatchers.IO.asExecutor(),
                    captureCallbacks
                )
            } else {
                cameraExtensionSession.capture(
                    captureBuilder.build(),
                    Dispatchers.IO.asExecutor(),
                    captureCallbacks
                )
            }
        } catch (e: CameraAccessException) {
            Log.v(TAG, "Camera failed to submit capture request!.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun submitRequest(
        templateType: Int,
        target: Surface,
        isRepeating: Boolean,
        block: (captureRequest: CaptureRequest.Builder) -> CaptureRequest.Builder) {
        return submitRequest(templateType, listOf(target), isRepeating, block)
    }

    private val captureCallbacks: CameraExtensionSession.ExtensionCaptureCallback =
        @RequiresApi(Build.VERSION_CODES.S)
        object : CameraExtensionSession.ExtensionCaptureCallback() {
            override fun onCaptureStarted(
                session: CameraExtensionSession, request: CaptureRequest,
                timestamp: Long
            ) {
                Log.v(TAG, "onCaptureStarted ts: $timestamp")
            }

            override fun onCaptureProcessStarted(
                session: CameraExtensionSession,
                request: CaptureRequest
            ) {
                Log.v(TAG, "onCaptureProcessStarted")
                // Turns to STILL_PROCESSING stage when the request tag is STILL_CAPTURE_TAG

            }

            override fun onCaptureResultAvailable(
                session: CameraExtensionSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                Log.v(TAG, "onCaptureResultAvailable")
            }

            override fun onCaptureFailed(
                session: CameraExtensionSession,
                request: CaptureRequest
            ) {
                Log.v(TAG, "onCaptureProcessFailed")
            }

            override fun onCaptureSequenceCompleted(
                session: CameraExtensionSession,
                sequenceId: Int
            ) {
                Log.v(TAG, "onCaptureProcessSequenceCompleted: $sequenceId")
            }

            override fun onCaptureSequenceAborted(
                session: CameraExtensionSession,
                sequenceId: Int
            ) {
                Log.v(TAG, "onCaptureProcessSequenceAborted: $sequenceId")
            }

            override fun onCaptureProcessProgressed(
                session: CameraExtensionSession,
                request: CaptureRequest,
                progress: Int,
            ) {
                Log.v(TAG, "onCaptureProcessProgressed: $progress")
            }
        }
}
