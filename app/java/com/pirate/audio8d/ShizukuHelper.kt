package com.pirate.audio8d

import android.content.Context
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

/**
 * Attempts to grant the CAPTURE_AUDIO_OUTPUT app op via Shizuku.
 * This can bypass the user consent dialog on some devices.
 */
object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    private const val PERMISSION_CODE = 1

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission() {
        if (!isAvailable()) return
        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            grantCaptureOp()
        } else {
            Shizuku.requestPermission(PERMISSION_CODE)
        }
    }

    private fun grantCaptureOp() {
        try {
            val process = Shizuku.newProcess(arrayOf("sh"), null, null)
            val packageName = "com.pirate.audio8d"
            // appops set <pkg> PROJECT_MEDIA allow
            val cmd = "appops set $packageName PROJECT_MEDIA allow\n"
            process.outputStream.write(cmd.toByteArray())
            process.outputStream.flush()
            process.waitFor()
            Log.i(TAG, "Shizuku: appops set completed.")
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku grant failed", e)
        }
    }

    fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == PERMISSION_CODE && grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            grantCaptureOp()
        }
    }
}