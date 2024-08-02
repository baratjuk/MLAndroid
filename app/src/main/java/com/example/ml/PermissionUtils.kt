package com.example.ml

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

val TAG = "PerrmissionUtils"
private val REQUIRED_RUNTIME_PERMISSIONS =
    arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
private val PERMISSION_REQUESTS = 1

public fun allRuntimePermissionsGranted(activity: Activity): Boolean {
    for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
        permission?.let {
            if (!isPermissionGranted(activity, it)) {
                return false
            }
        }
    }
    return true
}

public fun getRuntimePermissions(activity: Activity) {
    val permissionsToRequest = ArrayList<String>()
    for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
        permission?.let {
            if (!isPermissionGranted(activity, it)) {
                permissionsToRequest.add(permission)
            }
        }
    }
    if (permissionsToRequest.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            activity,
            permissionsToRequest.toTypedArray(),
            PERMISSION_REQUESTS
        )
    }
}

public fun isPermissionGranted(context: Context, permission: String): Boolean {
    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        Log.i(TAG, "Permission granted: $permission")
        return true
    }
    Log.i(TAG, "Permission NOT granted: $permission")
    return false
}