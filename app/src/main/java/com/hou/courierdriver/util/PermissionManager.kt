package com.hou.courierdriver.util

import android.app.Activity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

object PermissionManager {

    inline fun requestPermission(
        activity: Activity,
        crossinline onGranted: () -> Unit,
        crossinline onDenied: (isPermanentlyDenied: Boolean) -> Unit,
        permission: String,
        @StringRes rationaleString: Int
    ) {
        Dexter.withContext(activity)
            .withPermission(permission)
            .withListener(object : PermissionListener {

                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    onGranted()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    AlertDialog.Builder(activity)
                        .setMessage(rationaleString)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok) { _, _ -> token?.continuePermissionRequest() }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> token?.cancelPermissionRequest() }
                        .show()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    onDenied(response?.isPermanentlyDenied == true)
                }
            })
            .onSameThread()
            .check()
    }
}