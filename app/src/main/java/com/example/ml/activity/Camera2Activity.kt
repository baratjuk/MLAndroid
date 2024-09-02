package com.example.ml.activity

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import com.example.ml.vm.Camera2ViewModel
import com.example.ml.businesLogic.Utils
import com.example.ml.businesLogic.allRuntimePermissionsGranted
import com.example.ml.businesLogic.getRuntimePermissions
import com.example.ml.databinding.ActivityCamera2Binding


class Camera2Activity : ComponentActivity() {
    val TAG = "ML.Camera2Activity"

    private lateinit var binding: ActivityCamera2Binding
    private lateinit var camera2ViewModel : Camera2ViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        binding = ActivityCamera2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        camera2ViewModel = object : Camera2ViewModel(this, binding.texture) {
            override fun onOpen(previewSize: Size) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    binding.texture.scaleY = Utils.yScale(this.context, previewSize)
                }
            }

            override fun onBitmap(bitmap: Bitmap) {
                binding.image1.setImageBitmap(bitmap)
            }

            override fun onClose() {

            }

            override fun onError(type: ErrorTypes, code: Int) {

            }
        }
        binding.texture.scaleX = 1.0f
        camera2ViewModel.open("0")

        binding.button1.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        try {
            camera2ViewModel.close()
        } catch (e: Throwable) {
            Log.e(TAG, "Error closing camera", e)
        } catch (e : Exception) {
        }
        super.onDestroy()
    }
}
