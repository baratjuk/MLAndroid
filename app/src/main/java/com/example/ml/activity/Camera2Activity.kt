package com.example.ml.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.ml.businesLogic.Camera2ViewModel
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

        if (!allRuntimePermissionsGranted(this)) {
            getRuntimePermissions(this)
        }

        binding = ActivityCamera2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        camera2ViewModel = object : Camera2ViewModel(this, binding.texture) {
            override fun onClose() {

            }

            override fun onError(type: ErrorTypes, code: Int) {

            }
        }
        camera2ViewModel.open("1")

        binding.button1.setOnClickListener {

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
